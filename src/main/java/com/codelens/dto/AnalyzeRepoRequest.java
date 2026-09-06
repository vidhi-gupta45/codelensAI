package com.codelens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRepoRequest {

    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\\.git)?$",
        message = "Invalid GitHub repository URL format. Example: https://github.com/owner/repo"
    )
    private String repoUrl;
}
