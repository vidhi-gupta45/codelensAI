package com.codelens.repository;

import com.codelens.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, UUID> {

    List<CodeChunk> findByRepoAnalysisId(UUID repoAnalysisId);

    List<CodeChunk> findByFileNodeId(UUID fileNodeId);

    void deleteByRepoAnalysisId(UUID repoAnalysisId);
}
