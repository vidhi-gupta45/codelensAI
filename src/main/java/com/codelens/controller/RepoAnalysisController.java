package com.codelens.controller;

import com.codelens.dto.AnalyzeRepoRequest;
import com.codelens.dto.AnalyzeRepoResponse;
import com.codelens.dto.ArchitectureResponse;
import com.codelens.entity.AnalysisJob;
import com.codelens.entity.FileNode;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.User;
import com.codelens.entity.enums.AnalysisStatus;
import com.codelens.entity.enums.JobStatus;
import com.codelens.repository.AnalysisJobRepository;
import com.codelens.repository.RepoAnalysisRepository;
import com.codelens.repository.UserRepository;
import com.codelens.service.AnalysisPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoAnalysisController {

    private final RepoAnalysisRepository repoAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final UserRepository userRepository;
    private final AnalysisPipelineService analysisPipelineService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeRepoResponse> analyzeRepository(@Valid @RequestBody AnalyzeRepoRequest request) {
        User user = getAuthenticatedUser();
        String repoUrl = request.getRepoUrl().trim();

        String[] parts = parseOwnerAndRepo(repoUrl);
        String owner = parts[0];
        String repoName = parts[1];

        RepoAnalysis repoAnalysis = repoAnalysisRepository.findByUserIdAndRepoUrl(user.getId(), repoUrl)
                .orElseGet(() -> RepoAnalysis.builder()
                        .user(user)
                        .repoUrl(repoUrl)
                        .owner(owner)
                        .repoName(repoName)
                        .status(AnalysisStatus.PENDING)
                        .build());

        repoAnalysis.setStatus(AnalysisStatus.PROCESSING);
        repoAnalysis = repoAnalysisRepository.saveAndFlush(repoAnalysis);

        AnalysisJob job = AnalysisJob.builder()
                .repoAnalysis(repoAnalysis)
                .status(JobStatus.QUEUED)
                .progress(0)
                .currentStage("Queued")
                .build();

        job = analysisJobRepository.saveAndFlush(job);

        log.info("Queued analysis job {} for repo {} (User: {})", job.getId(), repoUrl, user.getEmail());

        analysisPipelineService.runPipelineAsync(job.getId());

        AnalyzeRepoResponse response = AnalyzeRepoResponse.builder()
                .jobId(job.getId())
                .repoId(repoAnalysis.getId())
                .repoUrl(repoUrl)
                .status(JobStatus.QUEUED)
                .message("Repository analysis job queued successfully")
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AnalysisJob> getJobStatus(@PathVariable UUID jobId) {
        AnalysisJob job = analysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis job not found with ID: " + jobId));
        return ResponseEntity.ok(job);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/{repoId}")
    public ResponseEntity<Map<String, Object>> getRepoAnalysis(@PathVariable UUID repoId) {
        User user = getAuthenticatedUser();
        RepoAnalysis repoAnalysis = repoAnalysisRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository analysis not found with ID: " + repoId));

        if (!repoAnalysis.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", repoAnalysis.getId());
        result.put("repoUrl", repoAnalysis.getRepoUrl());
        result.put("owner", repoAnalysis.getOwner());
        result.put("repoName", repoAnalysis.getRepoName());
        result.put("status", repoAnalysis.getStatus());
        result.put("summary", repoAnalysis.getSummary());
        result.put("lastAnalyzedAt", repoAnalysis.getLastAnalyzedAt());
        result.put("totalFiles", repoAnalysis.getFiles().size());
        result.put("totalChunks", repoAnalysis.getChunks().size());
        result.put("createdAt", repoAnalysis.getCreatedAt());

        return ResponseEntity.ok(result);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/{repoId}/architecture")
    public ResponseEntity<ArchitectureResponse> getArchitectureView(@PathVariable UUID repoId) {
        User user = getAuthenticatedUser();
        RepoAnalysis repoAnalysis = repoAnalysisRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository analysis not found with ID: " + repoId));

        if (!repoAnalysis.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Set<String> techStackSet = new HashSet<>();
        Map<String, List<String>> layers = new HashMap<>();
        layers.put("API Layer", new ArrayList<>());
        layers.put("Business Logic Layer", new ArrayList<>());
        layers.put("Data Access Layer", new ArrayList<>());
        layers.put("Data Model Layer", new ArrayList<>());
        layers.put("UI / Frontend Layer", new ArrayList<>());

        List<String> entryPoints = new ArrayList<>();
        List<ArchitectureResponse.FileNodeDto> fileTree = new ArrayList<>();

        for (FileNode file : repoAnalysis.getFiles()) {
            if (file.getLanguage() != null && !file.getLanguage().isBlank()) {
                techStackSet.add(file.getLanguage());
            }

            String pathLower = file.getPath().toLowerCase();
            if (pathLower.contains("controller") || pathLower.contains("routes") || pathLower.contains("api")) {
                layers.get("API Layer").add(file.getPath());
            } else if (pathLower.contains("service") || pathLower.contains("usecase")) {
                layers.get("Business Logic Layer").add(file.getPath());
            } else if (pathLower.contains("repository") || pathLower.contains("dao")) {
                layers.get("Data Access Layer").add(file.getPath());
            } else if (pathLower.contains("entity") || pathLower.contains("model") || pathLower.contains("dto")) {
                layers.get("Data Model Layer").add(file.getPath());
            } else if (pathLower.contains("component") || pathLower.contains("pages") || pathLower.contains("views")) {
                layers.get("UI / Frontend Layer").add(file.getPath());
            }

            if (pathLower.contains("main") || pathLower.contains("app") || pathLower.contains("index") || pathLower.contains("server")) {
                entryPoints.add(file.getPath());
            }

            fileTree.add(ArchitectureResponse.FileNodeDto.builder()
                    .id(file.getId())
                    .path(file.getPath())
                    .name(file.getName())
                    .language(file.getLanguage())
                    .size(file.getSize())
                    .isDirectory(file.isDirectory())
                    .parentPath(file.getParentPath())
                    .build());
        }

        ArchitectureResponse response = ArchitectureResponse.builder()
                .repoId(repoAnalysis.getId())
                .repoUrl(repoAnalysis.getRepoUrl())
                .owner(repoAnalysis.getOwner())
                .repoName(repoAnalysis.getRepoName())
                .summary(repoAnalysis.getSummary())
                .techStack(new ArrayList<>(techStackSet))
                .layers(layers)
                .entryPoints(entryPoints)
                .fileTree(fileTree)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RepoAnalysis>> getUserRepositories() {
        User user = getAuthenticatedUser();
        List<RepoAnalysis> repos = repoAnalysisRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        return ResponseEntity.ok(repos);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated user");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private String[] parseOwnerAndRepo(String repoUrl) {
        String cleanUrl = repoUrl.replace(".git", "");
        String[] parts = cleanUrl.split("/");
        if (parts.length >= 2) {
            String repo = parts[parts.length - 1];
            String owner = parts[parts.length - 2];
            return new String[]{owner, repo};
        }
        return new String[]{"unknown", "unknown"};
    }
}
