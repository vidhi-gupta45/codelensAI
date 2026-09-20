package com.codelens.repository;

import com.codelens.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {

    @Query("SELECT c FROM CodeChunk c JOIN FETCH c.fileNode WHERE c.repoAnalysis.id = :repoAnalysisId")
    List<CodeChunk> findByRepoAnalysisIdWithFileNode(@Param("repoAnalysisId") UUID repoAnalysisId);

    List<CodeChunk> findByRepoAnalysisId(UUID repoAnalysisId);

    List<CodeChunk> findByFileNodeId(UUID fileNodeId);

    void deleteByRepoAnalysisId(UUID repoAnalysisId);
}
