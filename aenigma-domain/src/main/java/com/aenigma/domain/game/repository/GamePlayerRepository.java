package com.aenigma.domain.game.repository;

import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {

    Optional<GamePlayer> findByGameIdAndUserId(UUID gameId, UUID userId);

    List<GamePlayer> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COUNT(gp) FROM GamePlayer gp JOIN gp.game g WHERE gp.user.id = :userId AND g.winnerTeam = gp.role")
    long countWinsByUserId(@Param("userId") UUID userId);

    long countByUserIdAndRole(UUID userId, GameRole role);
}
