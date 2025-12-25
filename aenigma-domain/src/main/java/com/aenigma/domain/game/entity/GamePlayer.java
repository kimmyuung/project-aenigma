package com.aenigma.domain.game.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 게임 참여자 엔티티
 * 
 * Game과 User의 N:M 매핑 테이블.
 * 게임 내 역할, 생존 여부, 투표 상태 등을 관리.
 */
@Entity
@Table(name = "game_players", indexes = {
        @Index(name = "idx_game_player_game", columnList = "game_id"),
        @Index(name = "idx_game_player_user", columnList = "user_id"),
        @Index(name = "idx_game_player_role", columnList = "role")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_game_user", columnNames = { "game_id", "user_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GamePlayer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 참여한 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 참여 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 게임 내 역할
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GameRole role;

    /**
     * 생존 여부
     */
    @Column(name = "is_alive", nullable = false)
    @Builder.Default
    private Boolean isAlive = true;

    // === Business Methods ===

    /**
     * 플레이어 제거 (사망)
     */
    public void eliminate() {
        this.isAlive = false;
    }

    /**
     * 범인 팀인지 확인
     */
    public boolean isCriminalTeam() {
        return role.isCriminalTeam();
    }

    /**
     * 시민 팀인지 확인
     */
    public boolean isCitizenTeam() {
        return role.isCitizenTeam();
    }

    // === Static Factory Methods ===

    public static GamePlayer create(Game game, User user, GameRole role) {
        return GamePlayer.builder()
                .game(game)
                .user(user)
                .role(role)
                .isAlive(true)
                .build();
    }
}
