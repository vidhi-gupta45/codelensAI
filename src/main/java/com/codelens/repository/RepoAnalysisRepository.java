package com.codelens.repository;

import com.codelens.entity.RepoAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepoAnalysisRepository extends JpaRepository<RepoAnalysis, UUID> {

    List<RepoAnalysis> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<RepoAnalysis> findByUserIdAndRepoUrl(UUID userId, String repoUrl);

    Optional<RepoAnalysis> findByRepoUrl(String repoUrl);

    boolean existsByUserIdAndRepoUrl(UUID userId, String repoUrl);
}
