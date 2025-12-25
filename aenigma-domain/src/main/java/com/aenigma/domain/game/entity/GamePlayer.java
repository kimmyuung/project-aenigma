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

    /**
     * 이번 턴 투표 완료 여부
     */
    @Column(name = "has_voted", nullable = false)
    @Builder.Default
    private Boolean hasVoted = false;

    /**
     * 이번 턴 스킬 사용 여부
     */
    @Column(name = "has_used_skill", nullable = false)
    @Builder.Default
    private Boolean hasUsedSkill = false;

    /**
     * 보호 상태 여부 (의사에 의해 보호됨)
     */
    @Column(name = "is_protected", nullable = false)
    @Builder.Default
    private Boolean isProtected = false;

    // === Business Methods ===

    /**
     * 플레이어 제거 (사망)
     */
    public void eliminate() {
        if (!this.isProtected) {
            this.isAlive = false;
        }
    }

    /**
     * 강제 제거 (투표에 의한 처형, 보호 무시)
     */
    public void execute() {
        this.isAlive = false;
    }

    /**
     * 보호 상태 설정
     */
    public void protect() {
        this.isProtected = true;
    }

    /**
     * 턴 초기화 (매 턴 시작시 호출)
     */
    public void resetTurn() {
        this.hasVoted = false;
        this.hasUsedSkill = false;
        this.isProtected = false;
    }

    /**
     * 투표 완료 처리
     */
    public void vote() {
        if (!this.isAlive) {
            throw new IllegalStateException("사망한 플레이어는 투표할 수 없습니다.");
        }
        this.hasVoted = true;
    }

    /**
     * 스킬 사용 처리
     */
    public void useSkill() {
        if (!this.isAlive) {
            throw new IllegalStateException("사망한 플레이어는 스킬을 사용할 수 없습니다.");
        }
        if (this.role == GameRole.CITIZEN) {
            throw new IllegalStateException("시민은 스킬이 없습니다.");
        }
        this.hasUsedSkill = true;
    }

    /**
     * 범인 팀인지 확인
     */
    public boolean isKillerTeam() {
        return role.isKillerTeam();
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
                .hasVoted(false)
                .hasUsedSkill(false)
                .isProtected(false)
                .build();
    }
}
