package com.codelens.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitCloneService {

    @Value("${app.repo.max-size-mb:50}")
    private long maxSizeMb;

    @Value("${app.repo.max-files:500}")
    private int maxFiles;

    @Value("${app.repo.temp-dir:#{systemProperties['java.io.tmpdir'] + '/codelens-repos'}}")
    private String baseTempDir;

    private static final Set<String> IGNORED_DIRECTORIES = new HashSet<>(Arrays.asList(
            ".git", "node_modules", "target", "build", "dist", ".idea", ".vscode",
            ".gradle", "__pycache__", ".mvn", "vendor", ".next", "out", "bin", "obj",
            ".pytest_cache", ".settings", ".classpath", ".project"
    ));

    private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "png", "jpg", "jpeg", "gif", "ico", "svg", "pdf", "zip", "gz", "tar", "rar", "7z",
            "jar", "war", "class", "exe", "dll", "so", "dylib", "pyc", "o", "a", "mp3", "mp4",
            "wav", "avi", "mov", "ttf", "woff", "woff2", "eot", "cur", "db", "sqlite", "psd",
            "ai", "ds_store", "lock", "icns"
    ));

    @Getter
    public static class GitCloneResult {
        private final Path tempDir;
        private final List<Path> files;

        public GitCloneResult(Path tempDir, List<Path> files) {
            this.tempDir = tempDir;
            this.files = files;
        }
    }

    public GitCloneResult cloneRepository(String repoUrl, UUID jobId) {
        log.info("Cloning repository {} for job {}", repoUrl, jobId);

        Path workDir = Paths.get(baseTempDir, jobId.toString());
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory for repository clone: " + e.getMessage(), e);
        }

        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(workDir.toFile())
                .setCloneAllBranches(false)
                .setDepth(1)
                .call()) {

            log.info("Repository cloned successfully into {}", workDir); // debugging purpose
        } catch (GitAPIException e) {
            cleanupTempDir(workDir);
            throw new IllegalArgumentException("Failed to clone repository from " + repoUrl + ": " + e.getMessage(), e);
        }

        List<Path> validFiles = new ArrayList<>();
        long[] totalSize = {0L};

        try {
            Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String folderName = dir.getFileName().toString();
                    if (IGNORED_DIRECTORIES.contains(folderName.toLowerCase())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isWorthAnalyzing(file)) {
                        validFiles.add(file);
                        totalSize[0] += attrs.size();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            cleanupTempDir(workDir);
            throw new RuntimeException("Failed to scan repository directory: " + e.getMessage(), e);
        }

        long maxSizeBytes = maxSizeMb * 1024 * 1024;
        if (validFiles.size() > maxFiles) {
            cleanupTempDir(workDir);
            throw new IllegalArgumentException(String.format(
                    "Repository exceeds maximum file count limit. Found %d files, max allowed is %d.",
                    validFiles.size(), maxFiles));
        }

        if (totalSize[0] > maxSizeBytes) {
            cleanupTempDir(workDir);
            throw new IllegalArgumentException(String.format(
                    "Repository exceeds maximum size limit. Size is %.2f MB, max allowed is %d MB.",
                    totalSize[0] / (1024.0 * 1024.0), maxSizeMb));
        }

        log.info("Repository verification passed: {} files, total size: {} bytes", validFiles.size(), totalSize[0]);
        return new GitCloneResult(workDir, validFiles);
    }

    private boolean isWorthAnalyzing(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        if (fileName.startsWith(".")) {
            return false;
        }

        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx != -1) {
            String ext = fileName.substring(dotIdx + 1);
            if (IGNORED_EXTENSIONS.contains(ext)) {
                return false;
            }
        }
        return true;
    }

    public void cleanupTempDir(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try {
            log.info("Cleaning up temporary directory: {}", tempDir);
            FileSystemUtils.deleteRecursively(tempDir);
        } catch (IOException e) {
            log.warn("Failed to delete temp directory {}: {}", tempDir, e.getMessage());
        }
    }
}
