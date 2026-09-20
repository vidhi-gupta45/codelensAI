package com.codelens.service;

import com.codelens.dto.ParsedProjectMetadata;
import com.codelens.entity.CodeChunk;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.enums.AnalysisStatus;
import com.codelens.repository.CodeChunkRepository;
import com.codelens.repository.RepoAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureSummaryService {

    private final ChatModel chatModel;
    private final RepoAnalysisRepository repoAnalysisRepository;
    private final CodeChunkRepository codeChunkRepository;

    /** Max code chunks to include in the Groq prompt. */
    private static final int MAX_CHUNKS = 16;

    /** Max characters per chunk snippet to keep token budget sane. */
    private static final int MAX_CHARS_PER_CHUNK = 1000;

    @Transactional
    public String generateSummary(RepoAnalysis repoAnalysis, ParsedProjectMetadata metadata) {
        log.info("Generating architecture summary for repo {}", repoAnalysis.getRepoUrl());

        String systemInstruction = """
                You are a principal software architect conducting an exhaustive, highly technical architectural review of a real-world software repository.
                You are provided with actual source code snippets and structural metadata extracted directly from the project codebase.

                Provide a structured, deep-dive architectural analysis formatted in clean, professional Markdown.

                ### REQUIRED STRUCTURE:

                # Executive Summary & Technology Stack
                - Identify the primary domain purpose and functionality of the application based on the source code.
                - List the primary language(s), framework(s) (e.g. FastAPI, Django, Flask, PyTorch, Spring Boot, React, Next.js, Express, Go/Gin, etc.), key libraries, and data storage technologies used.
                - Reference specific file paths, classes, module names, or package definitions visible in the provided code chunks.

                # Entry Points & Execution Lifecycle
                - Identify how execution begins (e.g. `app.py`, `main.py`, `manage.py`, `index.ts`, `Application.java`, CLI scripts, or route handlers).
                - Detail the step-by-step lifecycle of a request, CLI invocation, or processing loop, referencing actual function/method signatures, decorators (e.g., `@app.get`, `@router`), and module interactions.

                # Architectural Layers & Design Patterns
                - Analyze the structural organization (e.g., Modular Scripts, Service Layer, MVC, Clean Architecture, Data Pipeline, Computer Vision / ML Workflow).
                - Explain how components communicate, pass data, and handle application state.
                - Call out specific design patterns and paradigms observed (e.g., Factory, Decorator pattern, Async/Await concurrency, Middleware, Repository pattern, Modular imports).

                # Technical Assessment & Code Insights
                - Evaluate code readability, modularity, separation of concerns, and error handling observed in the snippets.
                - Highlight critical internal dependencies and external integrations.
                - Provide actionable architectural observations or potential refactoring insights grounded directly in the source code.

                IMPORTANT GUIDELINES:
                - Do NOT give generic or vague placeholder responses.
                - Continuously cite exact file paths, class names, function signatures (`def ...`, `class ...`), and code structures visible in the snippets.
                - Tailor your analysis specifically to the language paradigm (e.g. Pythonic decorators/modules for Python, OOP for Java, Async Event-Loop for JS/TS).
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("=== REPOSITORY METADATA ===\n");
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

        userPrompt.append("\nArchitectural Layers (with key file paths):\n");
        for (Map.Entry<String, List<String>> entry : metadata.getLayers().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                userPrompt.append("- ").append(entry.getKey())
                        .append(" (").append(entry.getValue().size()).append(" files):\n");
                // Show up to 8 actual file paths per layer
                entry.getValue().stream().limit(8)
                        .forEach(f -> userPrompt.append("    • ").append(f).append("\n"));
            }
        }

        userPrompt.append("\nTop Import Dependencies (file → what it depends on):\n");
        int depCount = 0;
        for (Map.Entry<String, List<String>> entry : metadata.getImportGraph().entrySet()) {
            if (depCount++ >= 12) break;
            userPrompt.append("- ").append(entry.getKey())
                    .append(" → ").append(String.join(", ", entry.getValue())).append("\n");
        }

        // -------------------------------------------------------
        // Attach actual code chunks from key architectural files
        // -------------------------------------------------------
        List<String> codeSnippets = selectRepresentativeChunks(repoAnalysis, metadata);
        if (!codeSnippets.isEmpty()) {
            userPrompt.append("\n=== ACTUAL SOURCE CODE CHUNKS ===\n");
            userPrompt.append("(Real code extracted from the most architecturally significant files. ")
                    .append("Use these to ground your analysis in what the code actually does.)\n\n");
            for (String snippet : codeSnippets) {
                userPrompt.append(snippet).append("\n");
            }
        } else {
            log.warn("No code chunks available yet for repo {} — summary will use metadata only", repoAnalysis.getId());
        }

        String fullPromptText = systemInstruction + "\n\n" + userPrompt;
        log.debug("Groq prompt length: {} characters", fullPromptText.length());

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
                    metadata.getEntryPoints().isEmpty() ? "Standard project structure"
                            : String.join(", ", metadata.getEntryPoints())
            );
            repoAnalysis.setSummary(fallbackSummary);
            repoAnalysis.setStatus(AnalysisStatus.COMPLETED);
            repoAnalysis.setLastAnalyzedAt(Instant.now());
            repoAnalysisRepository.save(repoAnalysis);

            return fallbackSummary;
        }
    }

    private List<String> selectRepresentativeChunks(RepoAnalysis repoAnalysis, ParsedProjectMetadata metadata) {
        List<CodeChunk> allChunks = codeChunkRepository.findByRepoAnalysisId(repoAnalysis.getId());
        if (allChunks.isEmpty()) {
            return List.of();
        }

        Map<String, List<CodeChunk>> chunksByFile = allChunks.stream()
                .filter(c -> c.getFileNode() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getFileNode().getPath(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted((a, b) -> Integer.compare(a.getChunkIndex(), b.getChunkIndex()))
                                        .collect(Collectors.toList())
                        )
                ));

        List<String> priorityFiles = buildPriorityFileList(metadata);

        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();

        for (String filePath : priorityFiles) {
            if (result.size() >= MAX_CHUNKS) break;
            if (seen.contains(filePath)) continue;
            seen.add(filePath);

            List<CodeChunk> fileChunks = chunksByFile.get(filePath);
            if (fileChunks == null || fileChunks.isEmpty()) continue;

            CodeChunk first = fileChunks.get(0);
            result.add(formatChunk(first, filePath));

            if (result.size() < MAX_CHUNKS && fileChunks.size() > 1
                    && isHighPriority(filePath, metadata)) {
                result.add(formatChunk(fileChunks.get(1), filePath));
            }
        }

        if (result.size() < MAX_CHUNKS) {
            for (Map.Entry<String, List<CodeChunk>> entry : chunksByFile.entrySet()) {
                if (result.size() >= MAX_CHUNKS) break;
                if (seen.contains(entry.getKey())) continue;
                List<CodeChunk> fc = entry.getValue();
                if (!fc.isEmpty() && fc.get(0).getTokenCount() != null && fc.get(0).getTokenCount() > 20) {
                    result.add(formatChunk(fc.get(0), entry.getKey()));
                    seen.add(entry.getKey());
                }
            }
        }

        log.info("Included {} code chunk(s) in Groq architecture prompt for repo {}", result.size(), repoAnalysis.getId());
        return result;
    }

    private List<String> buildPriorityFileList(ParsedProjectMetadata metadata) {
        List<String> ordered = new ArrayList<>();

        metadata.getEntryPoints().stream()
                .map(e -> e.contains(" (") ? e.substring(0, e.lastIndexOf(" (")) : e)
                .forEach(ordered::add);

        for (String layer : List.of("API Layer", "Business Logic Layer", "Data Access Layer", "Data Model Layer")) {
            List<String> layerFiles = metadata.getLayers().getOrDefault(layer, List.of());
            ordered.addAll(layerFiles);
        }

        return ordered;
    }

    private boolean isHighPriority(String filePath, ParsedProjectMetadata metadata) {
        String path = filePath.toLowerCase();
        return path.contains("controller") || path.contains("service")
                || path.contains("application") || path.contains("main");
    }

    private String formatChunk(CodeChunk chunk, String filePath) {
        String content = chunk.getContent();
        if (content.length() > MAX_CHARS_PER_CHUNK) {
            content = content.substring(0, MAX_CHARS_PER_CHUNK) + "\n... [truncated]";
        }
        return String.format(
                "--- File: %s (lines %d–%d) ---\n```\n%s\n```\n",
                filePath,
                chunk.getStartLine(),
                chunk.getEndLine(),
                content
        );
    }
}
