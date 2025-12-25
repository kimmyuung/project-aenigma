package com.aenigma.domain.room.repository;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Room Repository
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    List<Room> findByStatus(RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.status = 'WAITING' AND r.password IS NULL ORDER BY r.createdAt DESC")
    List<Room> findPublicWaitingRooms();

    List<Room> findByHostId(UUID hostId);

    @Query("SELECT r FROM Room r WHERE r.status = 'WAITING' AND SIZE(r.members) < r.maxPlayers ORDER BY r.createdAt DESC")
    List<Room> findJoinableRooms();

    @Query("SELECT r FROM Room r WHERE r.status = 'WAITING' AND r.title LIKE %:keyword% ORDER BY r.createdAt DESC")
    List<Room> searchByTitle(@Param("keyword") String keyword);
}
