package com.codelens.dto;

import com.codelens.entity.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRepoResponse {

    private UUID jobId;
    private UUID repoId;
    private String repoUrl;
    private JobStatus status;
    private String message;
}
