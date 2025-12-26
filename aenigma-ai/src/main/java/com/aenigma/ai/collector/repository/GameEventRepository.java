package com.aenigma.ai.collector.repository;

import com.aenigma.ai.collector.entity.GameEvent;
import com.aenigma.ai.collector.entity.GameEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 게임 이벤트 Repository
 */
@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, UUID> {

        /**
         * 특정 게임의 모든 이벤트 조회 (시간순)
         */
        List<GameEvent> findByGameIdOrderByCreatedAtAsc(UUID gameId);

        /**
         * 특정 게임의 특정 유형 이벤트 조회
         */
        List<GameEvent> findByGameIdAndEventTypeOrderByCreatedAtAsc(UUID gameId, GameEventType eventType);

        /**
         * 특정 플레이어의 이벤트 조회
         */
        List<GameEvent> findByGameIdAndPlayerIdOrderByCreatedAtAsc(UUID gameId, UUID playerId);

        /**
         * /**
         * 채팅/밀담 이벤트만 조회 (AI 학습용)
         * INVESTIGATION 이후 페이즈만 포함 (INTRO, LOBBY 제외)
         */
        @Query("SELECT e FROM GameEvent e WHERE e.gameId = :gameId " +
                        "AND e.eventType IN ('CHAT_MESSAGE', 'WHISPER') " +
                        "AND e.phase NOT IN ('INTRO', 'LOBBY') " +
                        "ORDER BY e.createdAt ASC")
        List<GameEvent> findChatEventsByGameId(@Param("gameId") UUID gameId);

        /**
         * 학습에 유의미한 이벤트만 조회 (INVESTIGATION 이후)
         */
        @Query("SELECT e FROM GameEvent e WHERE e.gameId = :gameId " +
                        "AND e.phase IN ('INVESTIGATION', 'FINAL_VOTE', 'CONCLUSION') " +
                        "ORDER BY e.createdAt ASC")
        List<GameEvent> findLearnableEventsByGameId(@Param("gameId") UUID gameId);

        /**
         * 투표 이벤트 조회
         */
        @Query("SELECT e FROM GameEvent e WHERE e.gameId = :gameId " +
                        "AND e.eventType = 'VOTE_CAST' " +
                        "ORDER BY e.round ASC, e.createdAt ASC")
        List<GameEvent> findVoteEventsByGameId(@Param("gameId") UUID gameId);

        /**
         * 특정 기간의 완료된 게임 이벤트 조회 (배치 학습용)
         */
        @Query("SELECT e FROM GameEvent e WHERE e.eventType = 'GAME_END' " +
                        "AND e.createdAt BETWEEN :start AND :end " +
                        "ORDER BY e.createdAt ASC")
        List<GameEvent> findCompletedGamesBetween(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * 특정 역할의 행동 패턴 조회 (AI 학습용)
         */
        @Query("SELECT e FROM GameEvent e WHERE e.playerRole = :role " +
                        "AND e.eventType IN ('CHAT_MESSAGE', 'WHISPER', 'VOTE_CAST') " +
                        "ORDER BY e.createdAt ASC")
        List<GameEvent> findEventsByRole(@Param("role") String role);

        /**
         * 게임 수 통계
         */
        @Query("SELECT COUNT(DISTINCT e.gameId) FROM GameEvent e WHERE e.eventType = 'GAME_END'")
        long countCompletedGames();

        /**
         * 총 이벤트 수
         */
        long countByGameId(UUID gameId);
}
