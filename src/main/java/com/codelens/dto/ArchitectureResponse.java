package com.codelens.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectureResponse {

    private UUID repoId;
    private String repoUrl;
    private String owner;
    private String repoName;
    private String summary;

    @Builder.Default
    private List<String> techStack = new ArrayList<>();

    @Builder.Default
    private Map<String, List<String>> layers = new HashMap<>();

    @Builder.Default
    private List<String> entryPoints = new ArrayList<>();

    @Builder.Default
    private List<FileNodeDto> fileTree = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileNodeDto {
        private UUID id;
        private String path;
        private String name;
        private String language;
        private Long size;
        private boolean isDirectory;
        private String parentPath;
    }
}
