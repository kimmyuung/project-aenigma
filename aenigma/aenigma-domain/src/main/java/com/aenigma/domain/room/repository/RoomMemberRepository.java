package com.aenigma.domain.room.repository;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RoomMember Repository
 */
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    List<RoomMember> findByRoom(Room room);

    long countByRoom(Room room);

    List<RoomMember> findByUser(User user);

    Optional<RoomMember> findByRoomAndUser(Room room, User user);

    boolean existsByRoomAndUser(Room room, User user);

    Optional<RoomMember> findByRoomAndIsHostTrue(Room room);

    long countByRoomAndIsReadyTrue(Room room);

    @Query("SELECT rm FROM RoomMember rm JOIN rm.room r WHERE rm.user = :user AND r.status IN ('WAITING', 'PLAYING')")
    List<RoomMember> findActiveRoomsByUser(@Param("user") User user);

    List<RoomMember> findByRoomAndIsConnectedTrue(Room room);

    void deleteByRoomAndUser(Room room, User user);
}
