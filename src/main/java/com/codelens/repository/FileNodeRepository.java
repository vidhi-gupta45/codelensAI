package com.codelens.repository;

import com.codelens.entity.FileNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileNodeRepository extends JpaRepository<FileNode, UUID> {

    List<FileNode> findByRepoAnalysisId(UUID repoAnalysisId);

    Optional<FileNode> findByRepoAnalysisIdAndPath(UUID repoAnalysisId, String path);

    void deleteByRepoAnalysisId(UUID repoAnalysisId);
}
