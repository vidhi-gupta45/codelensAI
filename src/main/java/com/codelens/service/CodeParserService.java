package com.codelens.service;

import com.codelens.dto.ParsedProjectMetadata;
import com.codelens.entity.FileNode;
import com.codelens.entity.RepoAnalysis;
import com.codelens.repository.FileNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeParserService {

    private final FileNodeRepository fileNodeRepository;

    private static final Pattern JAVA_IMPORT = Pattern.compile("^\\s*import\\s+([a-zA-Z0-9_.]+);");
    private static final Pattern PYTHON_IMPORT = Pattern.compile("^\\s*(?:from\\s+([a-zA-Z0-9_.]+)\\s+import|import\\s+([a-zA-Z0-9_.]+))");
    private static final Pattern JS_IMPORT = Pattern.compile("(?:import\\s+.*\\s+from\\s+['\"]([^'\"]+)['\"]|require\\(['\"]([^'\"]+)['\"]\\))");

    @Transactional
    public ParsedProjectMetadata parseRepository(RepoAnalysis repoAnalysis, Path tempDir, List<Path> files) {
        log.info("Parsing repository structure for {}", repoAnalysis.getRepoUrl());

        Set<String> techStack = new HashSet<>();
        List<String> entryPoints = new ArrayList<>();
        Map<String, List<String>> layers = new HashMap<>();
        Map<String, List<String>> importGraph = new HashMap<>();

        layers.put("API Layer", new ArrayList<>());
        layers.put("Business Logic Layer", new ArrayList<>());
        layers.put("Data Access Layer", new ArrayList<>());
        layers.put("Data Model Layer", new ArrayList<>());
        layers.put("UI / Frontend Layer", new ArrayList<>());

        List<FileNode> fileNodesToSave = new ArrayList<>();
        int totalLines = 0;

        for (Path file : files) {
            String relativePath = tempDir.relativize(file).toString().replace('\\', '/');
            String fileName = file.getFileName().toString();
            String extension = getExtension(fileName);
            String language = getLanguageFromExtension(extension);
            long size = 0;

            try {
                size = Files.size(file);
            } catch (IOException e) {
                log.warn("Could not read file size for {}", relativePath);
            }

            detectTechStackFromManifest(fileName, techStack);

            String parentPath = file.getParent() != null && !file.getParent().equals(tempDir)
                    ? tempDir.relativize(file.getParent()).toString().replace('\\', '/')
                    : "";

            FileNode node = FileNode.builder()
                    .repoAnalysis(repoAnalysis)
                    .path(relativePath)
                    .name(fileName)
                    .language(language)
                    .size(size)
                    .isDirectory(false)
                    .parentPath(parentPath)
                    .build();

            fileNodesToSave.add(node);

            if (isTextSourceFile(fileName, extension)) {
                try {
                    List<String> lines = Files.readAllLines(file);
                    totalLines += lines.size();

                    String content = String.join("\n", lines);

                    detectEntryPoint(relativePath, content, entryPoints);

                    detectLayers(relativePath, content, layers);

                    extractImports(relativePath, lines, language, importGraph);

                } catch (Exception e) {
                    log.warn("Failed to parse content for file {}: {}", relativePath, e.getMessage());
                }
            }
        }

        if (techStack.isEmpty()) {
            techStack.add("General Codebase");
        }

        fileNodeRepository.deleteByRepoAnalysisId(repoAnalysis.getId());
        List<FileNode> savedNodes = fileNodeRepository.saveAll(fileNodesToSave);

        ParsedProjectMetadata metadata = ParsedProjectMetadata.builder()
                .techStack(new ArrayList<>(techStack))
                .entryPoints(entryPoints)
                .layers(layers)
                .importGraph(importGraph)
                .totalFiles(savedNodes.size())
                .totalSize(savedNodes.stream().mapToLong(n -> n.getSize() != null ? n.getSize() : 0L).sum())
                .totalLines(totalLines)
                .build();

        log.info("Code parsing completed for {}. Discovered {} files, {} tech stack items, {} entry points.",
                repoAnalysis.getRepoUrl(), metadata.getTotalFiles(), metadata.getTechStack().size(), metadata.getEntryPoints().size());

        return metadata;
    }

    private void detectTechStackFromManifest(String fileName, Set<String> techStack) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.equals("pom.xml")) {
            techStack.add("Java");
            techStack.add("Maven");
        } else if (lowerName.equals("build.gradle") || lowerName.equals("build.gradle.kts")) {
            techStack.add("Java");
            techStack.add("Gradle");
        } else if (lowerName.equals("package.json")) {
            techStack.add("Node.js");
            techStack.add("JavaScript/TypeScript");
        } else if (lowerName.equals("requirements.txt") || lowerName.equals("pyproject.toml") || lowerName.equals("pipfile")) {
            techStack.add("Python");
        } else if (lowerName.equals("go.mod")) {
            techStack.add("Go");
        } else if (lowerName.equals("gemfile")) {
            techStack.add("Ruby");
        } else if (lowerName.equals("cargo.toml")) {
            techStack.add("Rust");
        } else if (lowerName.equals("dockerfile") || lowerName.endsWith(".dockerfile")) {
            techStack.add("Docker");
        }
    }

    private void detectEntryPoint(String relativePath, String content, List<String> entryPoints) {
        String lowerPath = relativePath.toLowerCase();
        if (content.contains("public static void main") || content.contains("@SpringBootApplication")) {
            entryPoints.add(relativePath + " (Java Main)");
        } else if (lowerPath.endsWith("index.js") || lowerPath.endsWith("index.ts") || lowerPath.endsWith("server.js") || lowerPath.endsWith("app.js")) {
            entryPoints.add(relativePath + " (Node/JS Entry)");
        } else if (lowerPath.endsWith("main.py") || lowerPath.endsWith("app.py") || lowerPath.endsWith("run.py") || lowerPath.endsWith("manage.py") ||
                content.contains("if __name__ == '__main__':") || content.contains("if __name__ == \"__main__\":") ||
                content.contains("FastAPI(") || content.contains("Flask(")) {
            entryPoints.add(relativePath + " (Python Entry)");
        } else if (lowerPath.endsWith("main.go") || (content.contains("package main") && content.contains("func main()"))) {
            entryPoints.add(relativePath + " (Go Main)");
        } else if (lowerPath.endsWith("app.tsx") || lowerPath.endsWith("app.jsx")) {
            entryPoints.add(relativePath + " (React App Entry)");
        }
    }

    private void detectLayers(String relativePath, String content, Map<String, List<String>> layers) {
        String lowerPath = relativePath.toLowerCase();

        if (lowerPath.contains("controller") || lowerPath.contains("routes") || lowerPath.contains("api") || lowerPath.contains("views") || lowerPath.contains("endpoints") ||
                content.contains("@RestController") || content.contains("@Controller") || content.contains("@app.get") || content.contains("@app.post") || content.contains("@router")) {
            layers.get("API Layer").add(relativePath);
        } else if (lowerPath.contains("service") || lowerPath.contains("usecase") || lowerPath.contains("handler") || lowerPath.contains("processor") || lowerPath.contains("pipeline") ||
                content.contains("@Service")) {
            layers.get("Business Logic Layer").add(relativePath);
        } else if (lowerPath.contains("repository") || lowerPath.contains("dao") || lowerPath.contains("db") || lowerPath.contains("crud") || content.contains("@Repository")) {
            layers.get("Data Access Layer").add(relativePath);
        } else if (lowerPath.contains("entity") || lowerPath.contains("model") || lowerPath.contains("dto") || lowerPath.contains("schema") || content.contains("@Entity") || content.contains("BaseModel")) {
            layers.get("Data Model Layer").add(relativePath);
        } else if (lowerPath.contains("component") || lowerPath.contains("pages") || lowerPath.contains("templates") || lowerPath.contains("views") || lowerPath.contains("src/ui")) {
            layers.get("UI / Frontend Layer").add(relativePath);
        }
    }

    private void extractImports(String relativePath, List<String> lines, String language, Map<String, List<String>> importGraph) {
        List<String> imports = new ArrayList<>();

        for (String line : lines) {
            if ("Java".equals(language)) {
                Matcher matcher = JAVA_IMPORT.matcher(line);
                if (matcher.find()) {
                    imports.add(matcher.group(1));
                }
            } else if ("Python".equals(language)) {
                Matcher matcher = PYTHON_IMPORT.matcher(line);
                if (matcher.find()) {
                    String imp = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    if (imp != null) imports.add(imp);
                }
            } else if ("JavaScript".equals(language) || "TypeScript".equals(language)) {
                Matcher matcher = JS_IMPORT.matcher(line);
                if (matcher.find()) {
                    String imp = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    if (imp != null) imports.add(imp);
                }
            }
        }

        if (!imports.isEmpty()) {
            importGraph.put(relativePath, imports);
        }
    }

    private boolean isTextSourceFile(String fileName, String extension) {
        return Arrays.asList(
                "java", "py", "js", "ts", "jsx", "tsx", "go", "rb", "rs", "c", "cpp", "h", "hpp",
                "cs", "php", "kt", "scala", "swift", "sql", "sh", "properties", "yml", "yaml",
                "xml", "json", "md", "txt", "html", "css", "scss"
        ).contains(extension.toLowerCase());
    }

    private String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx != -1 ? fileName.substring(idx + 1) : "";
    }

    private String getLanguageFromExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "java" -> "Java";
            case "py" -> "Python";
            case "js" -> "JavaScript";
            case "ts" -> "TypeScript";
            case "jsx" -> "JavaScript (React)";
            case "tsx" -> "TypeScript (React)";
            case "go" -> "Go";
            case "rb" -> "Ruby";
            case "rs" -> "Rust";
            case "c", "h" -> "C";
            case "cpp", "hpp" -> "C++";
            case "cs" -> "C#";
            case "php" -> "PHP";
            case "kt" -> "Kotlin";
            case "sql" -> "SQL";
            case "html" -> "HTML";
            case "css", "scss" -> "CSS";
            case "json" -> "JSON";
            case "xml" -> "XML";
            case "yml", "yaml" -> "YAML";
            case "md" -> "Markdown";
            default -> "Text";
        };
    }
}
