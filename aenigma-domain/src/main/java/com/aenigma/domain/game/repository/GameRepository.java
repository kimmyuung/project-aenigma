package com.aenigma.domain.game.repository;

import com.aenigma.domain.game.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    long countByRoomId(UUID roomId);

    @Query("SELECT g FROM Game g WHERE g.room.id = :roomId AND g.phase <> 'FINISHED'")
    Optional<Game> findActiveGameByRoomId(@Param("roomId") UUID roomId);

    List<Game> findByRoomIdOrderByRoundNumberDesc(UUID roomId);
}
