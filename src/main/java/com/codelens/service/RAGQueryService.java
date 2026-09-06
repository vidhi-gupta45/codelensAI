package com.codelens.service;

import com.codelens.dto.AskResponse;
import com.codelens.entity.CodeChunk;
import com.codelens.entity.QAHistory;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.User;
import com.codelens.repository.CodeChunkRepository;
import com.codelens.repository.QAHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGQueryService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final CodeChunkRepository codeChunkRepository;
    private final QAHistoryRepository qaHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Transactional
    public AskResponse askQuestion(RepoAnalysis repoAnalysis, User user, String question) {
        String trimmedQuestion = question.trim();
        String questionHash = hashString(repoAnalysis.getId() + ":" + trimmedQuestion.toLowerCase());
        String cacheKey = "rag_cache:" + repoAnalysis.getId() + ":" + questionHash;

        // Step 1: Check Redis cache
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("Redis RAG cache HIT for question on repo {}", repoAnalysis.getId());
                return objectMapper.readValue(cachedJson, AskResponse.class);
            }
        } catch (Exception e) {
            log.warn("Redis RAG cache read error: {}", e.getMessage());
        }

        // Step 2: Vector similarity search
        List<Document> similarDocs = new ArrayList<>();
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(trimmedQuestion)
                    .topK(4)
                    .filterExpression("repoId == '" + repoAnalysis.getId().toString() + "'")
                    .build();

            similarDocs = vectorStore.similaritySearch(searchRequest);
            log.info("PgVector similarity search returned {} documents for repo {}", similarDocs.size(), repoAnalysis.getId());
        } catch (Exception e) {
            log.warn("PgVector similarity search failed or empty ({}), attempting fallback chunk retrieval", e.getMessage());
        }

        // Fallback: If vectorStore returned no docs, load chunks directly from DB
        List<AskResponse.Citation> citations = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        if (!similarDocs.isEmpty()) {
            int idx = 1;
            for (Document doc : similarDocs) {
                Map<String, Object> meta = doc.getMetadata();
                String filePath = meta.getOrDefault("filePath", "Unknown").toString();
                Integer startLine = meta.containsKey("startLine") ? Integer.parseInt(meta.get("startLine").toString()) : 1;
                Integer endLine = meta.containsKey("endLine") ? Integer.parseInt(meta.get("endLine").toString()) : 1;

                contextBuilder.append(String.format("### Code Chunk %d (File: %s, Lines: %d-%d)\n", idx++, filePath, startLine, endLine));
                contextBuilder.append("```\n").append(doc.getText()).append("\n```\n\n");

                citations.add(AskResponse.Citation.builder()
                        .filePath(filePath)
                        .startLine(startLine)
                        .endLine(endLine)
                        .build());
            }
        } else {
            List<CodeChunk> chunks = codeChunkRepository.findByRepoAnalysisId(repoAnalysis.getId());
            int limit = Math.min(chunks.size(), 4);
            for (int i = 0; i < limit; i++) {
                CodeChunk chunk = chunks.get(i);
                String filePath = chunk.getFileNode().getPath();
                contextBuilder.append(String.format("### Code Chunk %d (File: %s, Lines: %d-%d)\n", i + 1, filePath, chunk.getStartLine(), chunk.getEndLine()));
                contextBuilder.append("```\n").append(chunk.getContent()).append("\n```\n\n");

                citations.add(AskResponse.Citation.builder()
                        .filePath(filePath)
                        .startLine(chunk.getStartLine())
                        .endLine(chunk.getEndLine())
                        .build());
            }
        }

        // Step 4: Build prompt & query LLM
        String systemInstruction = """
                You are CodeLens AI, an expert AI software architect and codebase Q&A assistant.
                Answer the user's question accurately using ONLY the provided code chunks as context.
                
                Guidelines:
                - Be concise, precise, and directly address the user's question.
                - Refer strictly to the provided file paths and line ranges when explaining logic.
                - If the provided code chunks do not contain enough context to answer the question, state so clearly.
                """;

        String fullPromptText = String.format("%s\n\nContext Chunks:\n%s\nUser Question: %s",
                systemInstruction, contextBuilder, trimmedQuestion);

        String answer;
        try {
            Prompt prompt = new Prompt(fullPromptText);
            answer = chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Groq LLM call failed during RAG Q&A: {}", e.getMessage(), e);
            answer = "I'm sorry, I could not generate an answer at this time due to an AI service error: " + e.getMessage();
        }

        // Remove duplicate citations while preserving order
        List<AskResponse.Citation> uniqueCitations = deduplicateCitations(citations);

        AskResponse response = AskResponse.builder()
                .answer(answer)
                .citations(uniqueCitations)
                .build();

        // Step 6: Save QAHistory
        try {
            String citationsJson = objectMapper.writeValueAsString(uniqueCitations);
            QAHistory qaHistory = QAHistory.builder()
                    .repoAnalysis(repoAnalysis)
                    .user(user)
                    .question(trimmedQuestion)
                    .answer(answer)
                    .citations(citationsJson)
                    .build();
            qaHistoryRepository.save(qaHistory);
        } catch (Exception e) {
            log.warn("Failed to persist QAHistory turn: {}", e.getMessage());
        }

        // Step 7: Cache response in Redis
        try {
            String jsonToCache = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonToCache, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache RAG response in Redis: {}", e.getMessage());
        }

        return response;
    }

    private List<AskResponse.Citation> deduplicateCitations(List<AskResponse.Citation> citations) {
        Map<String, AskResponse.Citation> map = new LinkedHashMap<>();
        for (AskResponse.Citation c : citations) {
            String key = c.getFilePath() + ":" + c.getStartLine() + "-" + c.getEndLine();
            map.putIfAbsent(key, c);
        }
        return new ArrayList<>(map.values());
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
