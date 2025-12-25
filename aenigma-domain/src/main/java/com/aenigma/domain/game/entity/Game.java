package com.aenigma.domain.game.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 게임 세션 엔티티
 * 
 * Room에서 시작된 실제 게임 인스턴스.
 * 한 Room에서 여러 Game이 진행될 수 있음 (재시작 가능).
 */
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_game_room", columnList = "room_id"),
        @Index(name = "idx_game_phase", columnList = "phase")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 게임이 진행된 방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * 해당 방에서 몇 번째 게임인지 (1부터 시작)
     */
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    /**
     * 현재 게임 단계
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 20)
    @Builder.Default
    private GamePhase phase = GamePhase.PREPARING;

    /**
     * 현재 일차 (낮/밤 사이클 카운트)
     */
    @Column(name = "day_count", nullable = false)
    @Builder.Default
    private Integer dayCount = 0;

    /**
     * 게임 시작 시각
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 게임 종료 시각
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * 승리 팀 (KILLER 또는 CITIZEN 역할 기준)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "winner_team", length = 20)
    private GameRole winnerTeam;

    /**
     * 게임 참여자 목록
     */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GamePlayer> players = new ArrayList<>();

    // === Business Methods ===

    /**
     * 게임 시작
     */
    public void start() {
        if (this.phase != GamePhase.PREPARING) {
            throw new IllegalStateException("준비 단계에서만 게임을 시작할 수 있습니다.");
        }
        this.phase = GamePhase.DAY;
        this.dayCount = 1;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 다음 단계로 진행
     */
    public void nextPhase() {
        switch (this.phase) {
            case DAY -> this.phase = GamePhase.VOTING;
            case VOTING -> {
                this.phase = GamePhase.NIGHT;
            }
            case NIGHT -> {
                this.phase = GamePhase.DAY;
                this.dayCount++;
            }
            case PREPARING, FINISHED -> throw new IllegalStateException("현재 단계에서는 진행할 수 없습니다.");
        }
    }

    /**
     * 게임 종료
     */
    public void finish(GameRole winnerTeam) {
        this.phase = GamePhase.FINISHED;
        this.finishedAt = LocalDateTime.now();
        this.winnerTeam = winnerTeam;
    }

    /**
     * 생존한 플레이어 수
     */
    public long getAlivePlayerCount() {
        return players.stream().filter(GamePlayer::getIsAlive).count();
    }

    /**
     * 생존한 범인 수
     */
    public long getAliveKillerCount() {
        return players.stream()
                .filter(p -> p.getIsAlive() && p.getRole().isKillerTeam())
                .count();
    }

    /**
     * 생존한 시민 팀 수
     */
    public long getAliveCitizenCount() {
        return players.stream()
                .filter(p -> p.getIsAlive() && p.getRole().isCitizenTeam())
                .count();
    }

    /**
     * 게임 진행 중인지 확인
     */
    public boolean isInProgress() {
        return phase != GamePhase.PREPARING && phase != GamePhase.FINISHED;
    }

    /**
     * 승리 조건 체크
     */
    public boolean checkWinCondition() {
        long aliveKillers = getAliveKillerCount();
        long aliveCitizens = getAliveCitizenCount();

        if (aliveKillers == 0) {
            finish(GameRole.CITIZEN);
            return true;
        }
        if (aliveKillers >= aliveCitizens) {
            finish(GameRole.KILLER);
            return true;
        }
        return false;
    }
}
