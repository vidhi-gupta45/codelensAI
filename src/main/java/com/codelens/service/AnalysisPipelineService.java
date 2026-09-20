package com.codelens.service;

import com.codelens.dto.ParsedProjectMetadata;
import com.codelens.entity.AnalysisJob;
import com.codelens.entity.RepoAnalysis;
import com.codelens.entity.enums.AnalysisStatus;
import com.codelens.entity.enums.JobStatus;
import com.codelens.repository.AnalysisJobRepository;
import com.codelens.repository.RepoAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisPipelineService {

    private final GitCloneService gitCloneService;
    private final CodeParserService codeParserService;
    private final CodeChunkingService codeChunkingService;
    private final EmbeddingService embeddingService;
    private final ArchitectureSummaryService architectureSummaryService;
    private final WebSocketNotificationService webSocketNotificationService;

    private final AnalysisJobRepository analysisJobRepository;
    private final RepoAnalysisRepository repoAnalysisRepository;

    @Async("taskExecutor")
    public void runPipelineAsync(UUID jobId) {
        log.info("Starting background analysis pipeline execution for job {}", jobId);

        AnalysisJob job = analysisJobRepository.findByIdWithRepoAnalysis(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis job not found: " + jobId));

        RepoAnalysis repoAnalysis = job.getRepoAnalysis();
        Path tempDir = null;

        try {
            job.setStartedAt(Instant.now());

            // ----------------------------------------------------
            // Step 1: CLONING
            // ----------------------------------------------------
            updateJobState(job, JobStatus.CLONING, 10, "Cloning Repository", "Cloning repository from GitHub...");
            repoAnalysis.setStatus(AnalysisStatus.PROCESSING);
            repoAnalysisRepository.saveAndFlush(repoAnalysis);

            GitCloneService.GitCloneResult cloneResult = gitCloneService.cloneRepository(repoAnalysis.getRepoUrl(), jobId);
            tempDir = cloneResult.getTempDir();

            // ----------------------------------------------------
            // Step 2: PARSING
            // ----------------------------------------------------
            updateJobState(job, JobStatus.PARSING, 30, "Parsing Structure",
                    String.format("Parsing codebase structure across %d files...", cloneResult.getFiles().size()));

            ParsedProjectMetadata metadata = codeParserService.parseRepository(repoAnalysis, tempDir, cloneResult.getFiles());

            // ----------------------------------------------------
            // Step 3: CHUNKING
            // ----------------------------------------------------
            updateJobState(job, JobStatus.CHUNKING, 50, "Chunking Source Code", "Splitting source files into structure-aware code chunks...");
            codeChunkingService.chunkRepositoryFiles(repoAnalysis, tempDir);

            // ----------------------------------------------------
            // Step 4: EMBEDDING
            // ----------------------------------------------------
            updateJobState(job, JobStatus.EMBEDDING, 75, "Generating Embeddings", "Generating 384-dimensional vector embeddings with Spring AI...");
            embeddingService.embedCodeChunks(repoAnalysis);

            // ----------------------------------------------------
            // Step 5: SUMMARIZING
            // ----------------------------------------------------
            updateJobState(job, JobStatus.SUMMARIZING, 90, "Generating Architectural Summary", "Generating architecture summary via Groq LLM...");
            architectureSummaryService.generateSummary(repoAnalysis, metadata);

            // ----------------------------------------------------
            // Step 6: COMPLETED
            // ----------------------------------------------------
            job.setCompletedAt(Instant.now());
            updateJobState(job, JobStatus.COMPLETED, 100, "Completed", "Repository analysis completed successfully!");

            log.info("Pipeline completed successfully for repo {} (job {})", repoAnalysis.getRepoUrl(), jobId);

        } catch (Exception e) {
            log.error("Pipeline failure for job {}: {}", jobId, e.getMessage(), e);

            job.setCompletedAt(Instant.now());
            job.setErrorMessage(e.getMessage());
            job.setStatus(JobStatus.FAILED);
            job.setCurrentStage("Failed");
            analysisJobRepository.saveAndFlush(job);

            repoAnalysis.setStatus(AnalysisStatus.FAILED);
            repoAnalysisRepository.saveAndFlush(repoAnalysis);

            webSocketNotificationService.notifyJobProgress(job, "Failed", "Analysis failed: " + e.getMessage(), e.getMessage());

        } finally {
            if (tempDir != null) {
                gitCloneService.cleanupTempDir(tempDir);
            }
        }
    }

    private void updateJobState(AnalysisJob job, JobStatus status, int progress, String stage, String message) {
        job.setStatus(status);
        job.setProgress(progress);
        job.setCurrentStage(stage);
        analysisJobRepository.saveAndFlush(job);

        webSocketNotificationService.notifyJobProgress(job, stage, message);
    }
}
