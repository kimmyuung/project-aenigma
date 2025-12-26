package com.aenigma.ai.collector.repository;

import com.aenigma.ai.collector.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게임 세션 Repository
 */
@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    /**
     * 게임 ID로 세션 조회
     */
    Optional<GameSession> findByGameId(UUID gameId);

    /**
     * 완료된 게임 세션 조회 (학습 데이터)
     */
    @Query("SELECT s FROM GameSession s WHERE s.finishedAt IS NOT NULL ORDER BY s.finishedAt DESC")
    List<GameSession> findCompletedSessions();

    /**
     * 시나리오별 게임 조회
     */
    List<GameSession> findByScenarioIdOrderByCreatedAtDesc(String scenarioId);

    /**
     * 특정 플레이어 수의 게임 조회
     */
    List<GameSession> findByPlayerCountOrderByCreatedAtDesc(int playerCount);

    /**
     * 승리 팀 통계
     */
    @Query("SELECT s.winningTeam, COUNT(s) FROM GameSession s " +
            "WHERE s.winningTeam IS NOT NULL GROUP BY s.winningTeam")
    List<Object[]> getWinningTeamStats();
}
