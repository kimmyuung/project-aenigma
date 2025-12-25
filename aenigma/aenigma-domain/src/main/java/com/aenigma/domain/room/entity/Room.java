package com.aenigma.domain.room.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 게임 방 엔티티
 */
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_code", columnList = "room_code", unique = true),
        @Index(name = "idx_room_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "room_code", unique = true, nullable = false, length = 10)
    private String roomCode;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @Column(name = "max_players", nullable = false)
    @Builder.Default
    private Integer maxPlayers = 6;

    @Column(name = "password", length = 100)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomMember> members = new ArrayList<>();

    public int getCurrentPlayerCount() {
        return members.size();
    }

    public boolean canJoin() {
        return status == RoomStatus.WAITING && getCurrentPlayerCount() < maxPlayers;
    }

    public boolean isPrivate() {
        return password != null && !password.isBlank();
    }

    public void startGame() {
        if (status != RoomStatus.WAITING) {
            throw new IllegalStateException("대기 상태에서만 게임을 시작할 수 있습니다");
        }
        this.status = RoomStatus.PLAYING;
        this.startedAt = LocalDateTime.now();
    }

    public void finishGame() {
        if (status != RoomStatus.PLAYING) {
            throw new IllegalStateException("진행 중인 게임만 종료할 수 있습니다");
        }
        this.status = RoomStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = RoomStatus.CLOSED;
    }

    public void changeHost(User newHost) {
        this.host = newHost;
    }
}
