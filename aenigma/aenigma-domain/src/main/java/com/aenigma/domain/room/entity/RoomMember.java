package com.aenigma.domain.room.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 방 참여자 엔티티
 */
@Entity
@Table(name = "room_members", indexes = {
        @Index(name = "idx_room_member_room", columnList = "room_id"),
        @Index(name = "idx_room_member_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_user", columnNames = { "room_id", "user_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_host", nullable = false)
    @Builder.Default
    private Boolean isHost = false;

    @Column(name = "is_ready", nullable = false)
    @Builder.Default
    private Boolean isReady = false;

    @Column(name = "is_connected", nullable = false)
    @Builder.Default
    private Boolean isConnected = true;

    public void toggleReady() {
        if (!this.isHost) {
            this.isReady = !this.isReady;
        }
    }

    public void promoteToHost() {
        this.isHost = true;
        this.isReady = true;
    }

    public void demoteFromHost() {
        this.isHost = false;
        this.isReady = false;
    }

    public void setConnectionStatus(boolean connected) {
        this.isConnected = connected;
    }

    public static RoomMember createMember(Room room, User user) {
        return RoomMember.builder()
                .room(room)
                .user(user)
                .isHost(false)
                .isReady(false)
                .build();
    }

    public static RoomMember createHost(Room room, User user) {
        return RoomMember.builder()
                .room(room)
                .user(user)
                .isHost(true)
                .isReady(true)
                .build();
    }
}
