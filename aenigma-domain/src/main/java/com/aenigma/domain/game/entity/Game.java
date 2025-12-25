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
    private GamePhase phase = GamePhase.INTRO;

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
        if (this.phase != GamePhase.INTRO) {
            // 생성 직후 상태가 INTRO가 아니라면(예: 생성시 PREPARING? -> 생성시 기본값 수정 필요)
            // GameBuilder에서 기본값을 INTRO로 하거나, 여기서 유연하게 처리.
            // 일단 Game 생성자/Builder에서 기본값을 INTRO로 변경해야 함.
            // 기존 코드: Builder.Default private GamePhase phase = GamePhase.PREPARING;
        }
        // 머더미스터리는 시작하면 바로 조사가 시작되거나, INTRO 단계에서 시작됨.
        // 여기서는 "게임 시작" 버튼을 누르면 시간이 흐르기 시작하는 것으로 간주.
        this.startedAt = LocalDateTime.now();
    }

    public void nextPhase() {
        switch (this.phase) {
            case INTRO -> this.phase = GamePhase.INVESTIGATION;
            case INVESTIGATION -> this.phase = GamePhase.FINAL_VOTE;
            case FINAL_VOTE -> this.phase = GamePhase.CONCLUSION;
            case CONCLUSION -> this.phase = GamePhase.FINISHED;
            case FINISHED -> throw new IllegalStateException("이미 종료된 게임입니다.");
        }

        // 단계 변경 시 시간 기록 등 추가 로직이 필요할 수 있음
    }

    /**
     * 게임 종료
     */
    /**
     * 게임 종료
     */
    public void finishGame(GameRole winnerTeam) {
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
    public long getAliveCriminalCount() {
        return players.stream()
                .filter(p -> p.getIsAlive() && p.getRole().isCriminalTeam())
                .count();
    }

    /**
     * 생존한 시민(용의자/탐정) 팀 수
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
        return phase != GamePhase.INTRO && phase != GamePhase.FINISHED;
    }

    /**
     * 승리 조건 체크 (일단 마피아 로직 유지하되 이름만 변경, 추후 머더미스터리 전용 승리 조건으로 교체 필요)
     */
    public boolean checkWinCondition() {
        if (phase != GamePhase.INVESTIGATION && phase != GamePhase.FINAL_VOTE) {
            return false;
        }

        long criminalCount = players.stream()
                .filter(GamePlayer::getIsAlive)
                .filter(GamePlayer::isCriminalTeam)
                .count();

        long citizenCount = players.stream()
                .filter(GamePlayer::getIsAlive)
                .filter(GamePlayer::isCitizenTeam)
                .count();

        if (criminalCount == 0) {
            finishGame(GameRole.SUSPECT); // 시민(탐정/용의자) 승리
            return true;
        }

        if (criminalCount >= citizenCount) {
            finishGame(GameRole.CRIMINAL); // 범인 승리
            return true;
        }

        return false;
    }
}
