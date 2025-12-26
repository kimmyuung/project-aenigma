package com.aenigma.ai.collector.repository;

import com.aenigma.ai.collector.entity.GameClue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 게임 단서 Repository
 */
@Repository
public interface GameClueRepository extends JpaRepository<GameClue, UUID> {

    /**
     * 특정 게임의 모든 단서 조회
     */
    List<GameClue> findByGameIdOrderByCreatedAtAsc(UUID gameId);

    /**
     * 특정 플레이어의 개별 단서 조회
     */
    List<GameClue> findByGameIdAndPlayerIdOrderByCreatedAtAsc(UUID gameId, UUID playerId);

    /**
     * 공개 단서만 조회
     */
    List<GameClue> findByGameIdAndIsPublicTrueOrderByCreatedAtAsc(UUID gameId);

    /**
     * 특정 페이즈에 공개된 단서 조회
     */
    List<GameClue> findByGameIdAndRevealedPhaseOrderByCreatedAtAsc(UUID gameId, String phase);

    /**
     * 특정 역할의 단서 조회 (AI 학습용)
     */
    @Query("SELECT c FROM GameClue c WHERE c.playerRole = :role ORDER BY c.createdAt ASC")
    List<GameClue> findByPlayerRole(@Param("role") String role);

    /**
     * 시나리오별 단서 조회 (패턴 분석용)
     */
    List<GameClue> findByScenarioIdOrderByCreatedAtAsc(String scenarioId);
}
