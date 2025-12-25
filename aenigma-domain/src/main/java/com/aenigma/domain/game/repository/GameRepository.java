package com.aenigma.domain.game.repository;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게임 Repository
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * 특정 방의 모든 게임 조회 (최신순)
     */
    List<Game> findByRoomIdOrderByRoundNumberDesc(UUID roomId);

    /**
     * 특정 방의 진행 중인 게임 조회
     */
    @Query("SELECT g FROM Game g WHERE g.room.id = :roomId AND g.phase NOT IN ('PREPARING', 'FINISHED')")
    Optional<Game> findActiveGameByRoomId(@Param("roomId") UUID roomId);

    /**
     * 특정 방에서 특정 단계의 게임 조회
     */
    List<Game> findByRoomIdAndPhase(UUID roomId, GamePhase phase);

    /**
     * 특정 방의 가장 최근 게임 조회
     */
    Optional<Game> findTopByRoomIdOrderByRoundNumberDesc(UUID roomId);

    /**
     * 특정 방의 총 게임 수
     */
    long countByRoomId(UUID roomId);

    /**
     * 특정 방에서 종료된 게임 수
     */
    long countByRoomIdAndPhase(UUID roomId, GamePhase phase);
}
