package com.codelens.repository;

import com.codelens.entity.AnalysisJob;
import com.codelens.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {

    @Query("SELECT j FROM AnalysisJob j JOIN FETCH j.repoAnalysis WHERE j.id = :id")
    Optional<AnalysisJob> findByIdWithRepoAnalysis(@Param("id") UUID id);

    Optional<AnalysisJob> findTopByRepoAnalysisIdOrderByCreatedAtDesc(UUID repoAnalysisId);

    List<AnalysisJob> findByRepoAnalysisIdOrderByCreatedAtDesc(UUID repoAnalysisId);

    List<AnalysisJob> findByStatus(JobStatus status);
}
