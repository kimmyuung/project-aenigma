package com.aenigma.domain.scenario.repository;

import com.aenigma.domain.scenario.entity.ScenarioReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 시나리오 리뷰 Repository
 */
@Repository
public interface ScenarioReviewRepository extends JpaRepository<ScenarioReview, UUID> {

    /**
     * 시나리오 리뷰 목록 (페이징)
     */
    Page<ScenarioReview> findByScenarioId(UUID scenarioId, Pageable pageable);

    /**
     * 사용자의 특정 시나리오 리뷰
     */
    Optional<ScenarioReview> findByReviewerIdAndScenarioId(UUID reviewerId, UUID scenarioId);

    /**
     * 리뷰 존재 여부
     */
    boolean existsByReviewerIdAndScenarioId(UUID reviewerId, UUID scenarioId);

    /**
     * 스포일러 제외 리뷰
     */
    Page<ScenarioReview> findByScenarioIdAndHasSpoilerFalse(UUID scenarioId, Pageable pageable);
}
