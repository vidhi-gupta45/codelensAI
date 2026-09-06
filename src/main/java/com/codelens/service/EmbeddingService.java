package com.codelens.service;

import com.codelens.entity.CodeChunk;
import com.codelens.entity.RepoAnalysis;
import com.codelens.repository.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final CodeChunkRepository codeChunkRepository;

    private static final int BATCH_SIZE = 15;

    @Transactional
    public void embedCodeChunks(RepoAnalysis repoAnalysis) {
        log.info("Generating vector embeddings for repo analysis {}", repoAnalysis.getId());

        List<CodeChunk> chunks = codeChunkRepository.findByRepoAnalysisId(repoAnalysis.getId());
        if (chunks.isEmpty()) {
            log.warn("No code chunks found to embed for repo analysis {}", repoAnalysis.getId());
            return;
        }

        List<Document> batchDocuments = new ArrayList<>();
        List<CodeChunk> batchChunks = new ArrayList<>();
        int embeddedCount = 0;

        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("repoId", repoAnalysis.getId().toString());
            metadata.put("filePath", chunk.getFileNode().getPath());
            metadata.put("startLine", chunk.getStartLine());
            metadata.put("endLine", chunk.getEndLine());
            metadata.put("chunkId", chunk.getId().toString());

            Document doc = new Document(chunk.getId().toString(), chunk.getContent(), metadata);
            batchDocuments.add(doc);
            batchChunks.add(chunk);

            if (batchDocuments.size() >= BATCH_SIZE || i == chunks.size() - 1) {
                try {
                    log.info("Adding batch of {} vector embeddings to PgVector (total progress {}/{})",
                            batchDocuments.size(), i + 1, chunks.size());

                    vectorStore.add(batchDocuments);

                    for (CodeChunk c : batchChunks) {
                        c.setVectorId(c.getId().toString());
                    }
                    codeChunkRepository.saveAll(batchChunks);

                    embeddedCount += batchDocuments.size();

                } catch (Exception e) {
                    log.error("Failed to generate or store vector embeddings for batch: {}", e.getMessage(), e);
                    throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
                } finally {
                    batchDocuments.clear();
                    batchChunks.clear();
                }
            }
        }

        log.info("Successfully generated and persisted {} vector embeddings for repo analysis {}", embeddedCount, repoAnalysis.getId());
    }
}
