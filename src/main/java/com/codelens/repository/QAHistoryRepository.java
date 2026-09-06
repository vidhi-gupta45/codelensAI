package com.codelens.repository;

import com.codelens.entity.QAHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QAHistoryRepository extends JpaRepository<QAHistory, UUID> {

    List<QAHistory> findByRepoAnalysisIdOrderByCreatedAtAsc(UUID repoAnalysisId);

    List<QAHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<QAHistory> findByRepoAnalysisIdAndQuestion(UUID repoAnalysisId, String question);
}
