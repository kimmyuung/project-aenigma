package com.aenigma.domain.game.repository;

import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게임 참여자 Repository
 */
@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {

    /**
     * 특정 게임의 모든 참여자 조회
     */
    List<GamePlayer> findByGameId(UUID gameId);

    /**
     * 특정 게임의 생존자만 조회
     */
    List<GamePlayer> findByGameIdAndIsAliveTrue(UUID gameId);

    /**
     * 특정 게임에서 특정 역할의 참여자 조회
     */
    List<GamePlayer> findByGameIdAndRole(UUID gameId, GameRole role);

    /**
     * 특정 게임에서 특정 사용자 조회
     */
    Optional<GamePlayer> findByGameIdAndUserId(UUID gameId, UUID userId);

    /**
     * 특정 사용자의 모든 게임 기록 조회
     */
    List<GamePlayer> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 특정 게임의 생존한 범인 수
     */
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.isAlive = true AND gp.role = 'KILLER'")
    long countAliveKillersByGameId(@Param("gameId") UUID gameId);

    /**
     * 특정 게임의 생존한 시민 팀 수
     */
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game.id = :gameId AND gp.isAlive = true AND gp.role != 'KILLER'")
    long countAliveCitizensByGameId(@Param("gameId") UUID gameId);

    /**
     * 특정 게임에서 투표하지 않은 생존자 수
     */
    long countByGameIdAndIsAliveTrueAndHasVotedFalse(UUID gameId);

    /**
     * 특정 사용자의 역할별 플레이 횟수
     */
    long countByUserIdAndRole(UUID userId, GameRole role);

    /**
     * 특정 사용자의 승리 횟수 (역할 기준)
     */
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.user.id = :userId AND gp.game.winnerTeam = gp.role")
    long countWinsByUserId(@Param("userId") UUID userId);
}
