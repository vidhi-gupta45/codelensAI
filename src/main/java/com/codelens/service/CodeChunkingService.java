package com.codelens.service;

import com.codelens.entity.CodeChunk;
import com.codelens.entity.FileNode;
import com.codelens.entity.RepoAnalysis;
import com.codelens.repository.CodeChunkRepository;
import com.codelens.repository.FileNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeChunkingService {

    private final CodeChunkRepository codeChunkRepository;
    private final FileNodeRepository fileNodeRepository;

    private static final int MAX_CHUNK_LINES = 80;
    private static final int OVERLAP_LINES = 10;

    @Transactional
    public List<CodeChunk> chunkRepositoryFiles(RepoAnalysis repoAnalysis, Path tempDir) {
        log.info("Starting code chunking for repository analysis {}", repoAnalysis.getId());

        List<FileNode> fileNodes = fileNodeRepository.findByRepoAnalysisId(repoAnalysis.getId());
        List<CodeChunk> allChunksToSave = new ArrayList<>();

        codeChunkRepository.deleteByRepoAnalysisId(repoAnalysis.getId());

        for (FileNode fileNode : fileNodes) {
            Path filePath = tempDir.resolve(fileNode.getPath());
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                continue;
            }

            try {
                List<String> lines = Files.readAllLines(filePath);
                if (lines.isEmpty()) {
                    continue;
                }

                List<CodeChunk> fileChunks = createChunksForFile(repoAnalysis, fileNode, lines);
                allChunksToSave.addAll(fileChunks);

            } catch (IOException e) {
                log.warn("Could not read lines for chunking in file {}: {}", fileNode.getPath(), e.getMessage());
            }
        }

        List<CodeChunk> savedChunks = codeChunkRepository.saveAll(allChunksToSave);
        log.info("Completed code chunking for {}. Created {} code chunks.", repoAnalysis.getRepoUrl(), savedChunks.size());

        return savedChunks;
    }

    private List<CodeChunk> createChunksForFile(RepoAnalysis repoAnalysis, FileNode fileNode, List<String> lines) {
        String lang = fileNode.getLanguage() != null ? fileNode.getLanguage().toLowerCase() : "";

        if (lang.contains("java") || lang.contains("javascript") || lang.contains("typescript") || lang.contains("go") || lang.contains("c")) {
            return chunkByBraces(repoAnalysis, fileNode, lines);
        } else if (lang.contains("python")) {
            return chunkByIndentation(repoAnalysis, fileNode, lines);
        } else {
            return chunkBySlidingWindow(repoAnalysis, fileNode, lines);
        }
    }

    private List<CodeChunk> chunkByBraces(RepoAnalysis repoAnalysis, FileNode fileNode, List<String> lines) {
        List<CodeChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;
        int currentStart = 1;
        int braceDepth = 0;
        List<String> currentChunkLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            currentChunkLines.add(line);

            braceDepth += countChar(line, '{') - countChar(line, '}');

            if ((braceDepth == 0 && !currentChunkLines.isEmpty() && currentChunkLines.size() >= 15) || currentChunkLines.size() >= MAX_CHUNK_LINES) {
                int endLine = i + 1;
                String content = String.join("\n", currentChunkLines);
                if (!content.trim().isEmpty()) {
                    chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx++, currentStart, endLine, content));
                }
                currentStart = endLine + 1;
                currentChunkLines.clear();
                braceDepth = 0;
            }
        }

        if (!currentChunkLines.isEmpty()) {
            String content = String.join("\n", currentChunkLines);
            if (!content.trim().isEmpty()) {
                chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx, currentStart, lines.size(), content));
            }
        }

        return chunks.isEmpty() ? chunkBySlidingWindow(repoAnalysis, fileNode, lines) : chunks;
    }

    private List<CodeChunk> chunkByIndentation(RepoAnalysis repoAnalysis, FileNode fileNode, List<String> lines) {
        List<CodeChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;
        int currentStart = 1;
        List<String> currentChunkLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if ((trimmed.startsWith("def ") || trimmed.startsWith("class ")) && !currentChunkLines.isEmpty() && currentChunkLines.size() >= 15) {
                int endLine = i;
                String content = String.join("\n", currentChunkLines);
                if (!content.trim().isEmpty()) {
                    chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx++, currentStart, endLine, content));
                }
                currentStart = i + 1;
                currentChunkLines.clear();
            }

            currentChunkLines.add(line);

            if (currentChunkLines.size() >= MAX_CHUNK_LINES) {
                int endLine = i + 1;
                String content = String.join("\n", currentChunkLines);
                if (!content.trim().isEmpty()) {
                    chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx++, currentStart, endLine, content));
                }
                currentStart = endLine + 1;
                currentChunkLines.clear();
            }
        }

        if (!currentChunkLines.isEmpty()) {
            String content = String.join("\n", currentChunkLines);
            if (!content.trim().isEmpty()) {
                chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx, currentStart, lines.size(), content));
            }
        }

        return chunks.isEmpty() ? chunkBySlidingWindow(repoAnalysis, fileNode, lines) : chunks;
    }

    private List<CodeChunk> chunkBySlidingWindow(RepoAnalysis repoAnalysis, FileNode fileNode, List<String> lines) {
        List<CodeChunk> chunks = new ArrayList<>();
        int chunkIdx = 0;
        int total = lines.size();
        int step = MAX_CHUNK_LINES - OVERLAP_LINES;

        for (int i = 0; i < total; i += step) {
            int endIdx = Math.min(i + MAX_CHUNK_LINES, total);
            List<String> sub = lines.subList(i, endIdx);
            String content = String.join("\n", sub);

            if (!content.trim().isEmpty()) {
                chunks.add(buildChunk(repoAnalysis, fileNode, chunkIdx++, i + 1, endIdx, content));
            }

            if (endIdx == total) {
                break;
            }
        }

        return chunks;
    }

    private CodeChunk buildChunk(RepoAnalysis repoAnalysis, FileNode fileNode, int index, int startLine, int endLine, String content) {
        int tokenCount = content.split("\\s+").length;

        return CodeChunk.builder()
                .repoAnalysis(repoAnalysis)
                .fileNode(fileNode)
                .chunkIndex(index)
                .startLine(startLine)
                .endLine(endLine)
                .content(content)
                .tokenCount(tokenCount)
                .build();
    }

    private int countChar(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) count++;
        }
        return count;
    }
}
