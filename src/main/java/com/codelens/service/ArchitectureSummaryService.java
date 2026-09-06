package com.codelens.service;

import com.codelens.dto.ParsedProjectMetadata;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.enums.AnalysisStatus;
import com.codelens.repository.RepoAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureSummaryService {

    private final ChatModel chatModel;
    private final RepoAnalysisRepository repoAnalysisRepository;

    @Transactional
    public String generateSummary(RepoAnalysis repoAnalysis, ParsedProjectMetadata metadata) {
        log.info("Generating architecture summary for repo {}", repoAnalysis.getRepoUrl());

        String systemInstruction = """
                You are a senior software architect analyzing a software repository.
                Based on the provided codebase metadata, write a clear, structured, and insightful summary of the repository architecture.
                
                Requirements for your response:
                - Write 3 to 4 well-structured paragraphs in clear English.
                - Paragraph 1: Overview of the project, primary purpose, language, frameworks, and tech stack discovered.
                - Paragraph 2: Core entry points, request flow, and architectural pattern (e.g., MVC, Layered, Microservice, Monolith).
                - Paragraph 3: Major architectural layers (API, Service, Repository, Database, Frontend) and how components interact.
                - Paragraph 4: General health assessment, key dependencies, and notable design insights.
                - Do NOT include markdown code blocks or bullet points. Respond ONLY in plain paragraph text.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Repository URL: ").append(repoAnalysis.getRepoUrl()).append("\n");
        userPrompt.append("Total Source Files: ").append(metadata.getTotalFiles()).append("\n");
        userPrompt.append("Total Lines of Code: ").append(metadata.getTotalLines()).append("\n");
        userPrompt.append("Tech Stack Discovered: ").append(String.join(", ", metadata.getTechStack())).append("\n\n");

        userPrompt.append("Entry Points:\n");
        if (metadata.getEntryPoints().isEmpty()) {
            userPrompt.append("- Standard framework conventions\n");
        } else {
            for (String entry : metadata.getEntryPoints()) {
                userPrompt.append("- ").append(entry).append("\n");
            }
        }
        userPrompt.append("\nArchitectural Layers:\n");
        for (Map.Entry<String, List<String>> entry : metadata.getLayers().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                userPrompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" files\n");
            }
        }

        userPrompt.append("\nTop Key File Dependencies:\n");
        int count = 0;
        for (Map.Entry<String, List<String>> entry : metadata.getImportGraph().entrySet()) {
            if (count++ >= 10) break;
            userPrompt.append("- ").append(entry.getKey()).append(" depends on ").append(String.join(", ", entry.getValue())).append("\n");
        }

        String fullPromptText = systemInstruction + "\n\n" + userPrompt;

        try {
            Prompt prompt = new Prompt(fullPromptText);
            String summaryText = chatModel.call(prompt).getResult().getOutput().getText();

            repoAnalysis.setSummary(summaryText);
            repoAnalysis.setStatus(AnalysisStatus.COMPLETED);
            repoAnalysis.setLastAnalyzedAt(Instant.now());
            repoAnalysisRepository.save(repoAnalysis);

            log.info("Successfully generated architecture summary for {}", repoAnalysis.getRepoUrl());
            return summaryText;

        } catch (Exception e) {
            log.error("Failed to generate AI architecture summary via Groq: {}", e.getMessage(), e);
            String fallbackSummary = String.format(
                    "Repository %s contains %d files (%d lines of code) built with %s. Entry points identified: %s.",
                    repoAnalysis.getRepoName() != null ? repoAnalysis.getRepoName() : repoAnalysis.getRepoUrl(),
                    metadata.getTotalFiles(),
                    metadata.getTotalLines(),
                    String.join(", ", metadata.getTechStack()),
                    metadata.getEntryPoints().isEmpty() ? "Standard project structure" : String.join(", ", metadata.getEntryPoints())
            );
            repoAnalysis.setSummary(fallbackSummary);
            repoAnalysis.setStatus(AnalysisStatus.COMPLETED);
            repoAnalysis.setLastAnalyzedAt(Instant.now());
            repoAnalysisRepository.save(repoAnalysis);

            return fallbackSummary;
        }
    }
}
