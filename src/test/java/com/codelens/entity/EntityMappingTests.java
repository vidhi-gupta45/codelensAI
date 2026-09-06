package com.codelens.entity;

import com.codelens.entity.enums.AnalysisStatus;
import com.codelens.entity.enums.JobStatus;
import com.codelens.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityMappingTests {

    @Test
    @DisplayName("Verify User and RepoAnalysis relationships")
    void testUserAndRepoAnalysis() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("developer@codelens.ai")
                .password("securePassword123")
                .role(UserRole.ROLE_USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertEquals(userId, user.getId());
        assertEquals("developer@codelens.ai", user.getEmail());
        assertEquals(UserRole.ROLE_USER, user.getRole());
        assertTrue(user.getRepoAnalyses().isEmpty());

        UUID repoId = UUID.randomUUID();
        RepoAnalysis repoAnalysis = RepoAnalysis.builder()
                .id(repoId)
                .user(user)
                .repoUrl("https://github.com/spring-projects/spring-boot")
                .owner("spring-projects")
                .repoName("spring-boot")
                .defaultBranch("main")
                .status(AnalysisStatus.PENDING)
                .build();

        assertEquals(repoId, repoAnalysis.getId());
        assertEquals(user, repoAnalysis.getUser());
        assertEquals(AnalysisStatus.PENDING, repoAnalysis.getStatus());
    }

    @Test
    @DisplayName("Verify AnalysisJob creation and status transitions")
    void testAnalysisJob() {
        UUID jobId = UUID.randomUUID();
        AnalysisJob job = AnalysisJob.builder()
                .id(jobId)
                .status(JobStatus.CLONING)
                .progress(25)
                .currentStage("Cloning repository from GitHub...")
                .startedAt(Instant.now())
                .build();

        assertEquals(JobStatus.CLONING, job.getStatus());
        assertEquals(25, job.getProgress());
        assertEquals("Cloning repository from GitHub...", job.getCurrentStage());
    }

    @Test
    @DisplayName("Verify FileNode and CodeChunk linkage for citations")
    void testFileNodeAndCodeChunk() {
        UUID fileId = UUID.randomUUID();
        FileNode fileNode = FileNode.builder()
                .id(fileId)
                .path("src/main/java/com/codelens/SecurityConfig.java")
                .name("SecurityConfig.java")
                .language("Java")
                .size(1024L)
                .isDirectory(false)
                .parentPath("src/main/java/com/codelens")
                .build();

        assertEquals("SecurityConfig.java", fileNode.getName());
        assertFalse(fileNode.isDirectory());

        UUID chunkId = UUID.randomUUID();
        CodeChunk chunk = CodeChunk.builder()
                .id(chunkId)
                .fileNode(fileNode)
                .chunkIndex(0)
                .startLine(12)
                .endLine(45)
                .content("@Configuration\npublic class SecurityConfig { ... }")
                .vectorId("doc-vector-uuid-1234")
                .tokenCount(80)
                .build();

        assertEquals(12, chunk.getStartLine());
        assertEquals(45, chunk.getEndLine());
        assertEquals(fileNode, chunk.getFileNode());
    }

    @Test
    @DisplayName("Verify QAHistory creation")
    void testQAHistory() {
        UUID qaId = UUID.randomUUID();
        QAHistory qa = QAHistory.builder()
                .id(qaId)
                .question("Where is the authentication configured?")
                .answer("Authentication is configured in SecurityConfig.java lines 12-45.")
                .citations("[{\"path\": \"SecurityConfig.java\", \"startLine\": 12, \"endLine\": 45}]")
                .createdAt(Instant.now())
                .build();

        assertEquals("Where is the authentication configured?", qa.getQuestion());
        assertNotNull(qa.getCitations());
    }
}
