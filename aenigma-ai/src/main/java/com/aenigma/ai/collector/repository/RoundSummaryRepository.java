package com.aenigma.ai.collector.repository;

import com.aenigma.ai.collector.entity.RoundSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 라운드 요약 레포지토리
 */
public interface RoundSummaryRepository extends JpaRepository<RoundSummary, UUID> {

    /**
     * 게임의 모든 라운드 요약 조회
     */
    List<RoundSummary> findByGameIdOrderByRoundAsc(UUID gameId);

    /**
     * 특정 게임, 라운드의 요약 조회
     */
    Optional<RoundSummary> findByGameIdAndRound(UUID gameId, int round);

    /**
     * 게임의 라운드 요약 개수
     */
    long countByGameId(UUID gameId);

    /**
     * 요약이 있는 게임 ID 목록
     */
    @Query("SELECT DISTINCT rs.gameId FROM RoundSummary rs")
    List<UUID> findDistinctGameIds();

    /**
     * 특정 phase의 요약만 조회
     */
    List<RoundSummary> findByGameIdAndPhaseOrderByRoundAsc(UUID gameId, String phase);
}
