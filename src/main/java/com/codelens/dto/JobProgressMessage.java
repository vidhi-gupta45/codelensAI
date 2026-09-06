package com.codelens.dto;

import com.codelens.entity.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressMessage {

    private UUID jobId;
    private UUID repoId;
    private JobStatus status;
    private Integer progress;
    private String stage;
    private String message;
    private String errorMessage;
    private Instant timestamp;
}
