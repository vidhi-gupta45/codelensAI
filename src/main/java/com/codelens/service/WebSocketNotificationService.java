package com.codelens.service;

import com.codelens.dto.JobProgressMessage;
import com.codelens.entity.AnalysisJob;
import com.codelens.entity.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyJobProgress(AnalysisJob job, String stageName, String message) {
        notifyJobProgress(job, stageName, message, null);
    }

    public void notifyJobProgress(AnalysisJob job, String stageName, String message, String errorMessage) {
        UUID jobId = job.getId();
        UUID repoId = job.getRepoAnalysis() != null ? job.getRepoAnalysis().getId() : null;

        JobProgressMessage payload = JobProgressMessage.builder()
                .jobId(jobId)
                .repoId(repoId)
                .status(job.getStatus())
                .progress(job.getProgress())
                .stage(stageName)
                .message(message)
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/jobs/" + jobId, payload);
            if (repoId != null) {
                messagingTemplate.convertAndSend("/topic/repos/" + repoId, payload);
            }
            log.debug("Sent WebSocket update for job {}: {} ({}%)", jobId, job.getStatus(), job.getProgress());
        } catch (Exception e) {
            log.warn("Could not send WebSocket notification for job {}: {}", jobId, e.getMessage());
        }
    }
}
