package com.aenigma.domain.game.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 게임 참가자 (GamePlayer)
 */
@Entity
@Table(name = "game_players", indexes = {
        @Index(name = "idx_gp_game", columnList = "game_id"),
        @Index(name = "idx_gp_user", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GamePlayer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 참여 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 사용자 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 게임 내 역할
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private GameRole role;

    /**
     * 생존 여부
     */
    @Column(name = "is_alive", nullable = false)
    @Builder.Default
    private Boolean isAlive = true;

    // --- Actions ---

    public static GamePlayer create(Game game, User user, GameRole role) {
        return GamePlayer.builder()
                .game(game)
                .user(user)
                .role(role)
                .isAlive(true)
                .build();
    }

    public void eliminate() {
        this.isAlive = false;
    }

    public boolean isCriminalTeam() {
        return this.role == GameRole.CRIMINAL;
    }

    public boolean isCitizenTeam() {
        return !isCriminalTeam();
    }
}
