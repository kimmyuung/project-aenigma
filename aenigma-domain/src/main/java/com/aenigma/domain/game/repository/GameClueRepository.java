package com.aenigma.domain.game.repository;

import com.aenigma.domain.game.entity.GameClue;
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
     * 게임의 모든 단서 조회
     */
    List<GameClue> findByGameId(UUID gameId);

    /**
     * 게임의 공개된 단서 조회
     */
    List<GameClue> findByGameIdAndIsDiscoveredTrue(UUID gameId);

    /**
     * 특정 플레이어에게 배정된 개인 단서 조회
     */
    List<GameClue> findByGameIdAndAssignedPlayerId(UUID gameId, UUID playerId);

    /**
     * 플레이어가 볼 수 있는 모든 단서 (공개 + 개인 + 발견한 것)
     */
    @Query("SELECT gc FROM GameClue gc WHERE gc.game.id = :gameId AND (" +
            "(gc.clueType = 'PUBLIC' AND gc.isDiscovered = true) OR " +
            "(gc.clueType = 'PERSONAL' AND gc.assignedPlayer.id = :playerId) OR " +
            "(gc.clueType = 'HIDDEN' AND gc.discoveredBy.id = :playerId))")
    List<GameClue> findVisibleClues(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);

    /**
     * 현재 라운드에서 발견 가능한 HIDDEN 단서
     */
    @Query("SELECT gc FROM GameClue gc WHERE gc.game.id = :gameId " +
            "AND gc.clueType = 'HIDDEN' AND gc.isDiscovered = false " +
            "AND (gc.revealRound IS NULL OR gc.revealRound <= :currentRound)")
    List<GameClue> findDiscoverableClues(@Param("gameId") UUID gameId, @Param("currentRound") int currentRound);
}
