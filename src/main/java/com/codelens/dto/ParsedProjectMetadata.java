package com.codelens.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedProjectMetadata {

    @Builder.Default
    private List<String> techStack = new ArrayList<>();

    @Builder.Default
    private List<String> entryPoints = new ArrayList<>();

    @Builder.Default
    private Map<String, List<String>> layers = new HashMap<>();

    @Builder.Default
    private Map<String, List<String>> importGraph = new HashMap<>();

    private int totalFiles;
    private long totalSize;
    private int totalLines;
}
