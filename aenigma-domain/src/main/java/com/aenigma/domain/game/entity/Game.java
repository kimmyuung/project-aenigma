package com.aenigma.domain.game.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.scenario.entity.Scenario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 게임 (Game)
 * 방에서 진행되는 한 판의 게임
 */
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_game_room", columnList = "room_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Game extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 게임이 진행되는 방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * 라운드 번호 (해당 방의 n번째 게임)
     */
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    /**
     * 현재 게임 진행 단계
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    @Builder.Default
    private GamePhase phase = GamePhase.INTRO;

    /**
     * 현재 일차 (Day) - 머더미스터리에서는 단계 진행 척도로 사용 가능 (1일차, 2일차...)
     */
    @Column(name = "day_count", nullable = false)
    @Builder.Default
    private Integer dayCount = 0;

    /**
     * 현재 조사 라운드 (1~3)
     */
    @Column(name = "investigation_round", nullable = false)
    @Builder.Default
    private Integer investigationRound = 1;

    /**
     * 최대 조사 라운드 수 (GM 설정, 1~3)
     */
    @Column(name = "max_investigation_rounds", nullable = false)
    @Builder.Default
    private Integer maxInvestigationRounds = 2;

    /**
     * 게임 시작 시간
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 게임 종료 시간
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * 승리 팀 (게임 종료 후 설정)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "winner_team")
    private GameRole winnerTeam;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GamePlayer> players = new ArrayList<>();

    /**
     * 게임에 사용된 시나리오 (선택적)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    /**
     * 게임 내 단서 목록
     */
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GameClue> clues = new ArrayList<>();

    public void start() {
        if (this.phase != GamePhase.INTRO) {
            throw new IllegalStateException("게임은 INTRO 단계에서만 시작할 수 있습니다.");
        }
        this.phase = GamePhase.LOBBY;
        this.startedAt = LocalDateTime.now();
        this.dayCount = 1;
        this.investigationRound = 1;
    }

    public void nextPhase() {
        switch (this.phase) {
            case INTRO:
                this.phase = GamePhase.LOBBY;
                break;
            case LOBBY:
                this.phase = GamePhase.INVESTIGATION;
                this.investigationRound = 1;
                break;
            case INVESTIGATION:
                // 조사 라운드가 남았으면 다음 라운드, 아니면 투표로
                if (this.investigationRound < this.maxInvestigationRounds) {
                    this.investigationRound++;
                    // 페이즈는 그대로 INVESTIGATION 유지
                } else {
                    this.phase = GamePhase.FINAL_VOTE;
                }
                break;
            case FINAL_VOTE:
                this.phase = GamePhase.CONCLUSION;
                break;
            case CONCLUSION:
                finishGame(null); // 무승부 또는 별도 처리
                break;
            case FINISHED:
                throw new IllegalStateException("이미 종료된 게임입니다.");
        }
    }

    /**
     * 현재 조사 라운드 정보 반환 (예: "2/3")
     */
    public String getInvestigationRoundInfo() {
        return this.investigationRound + "/" + this.maxInvestigationRounds;
    }

    /**
     * 최대 조사 라운드 수 설정 (1~3)
     */
    public void setMaxInvestigationRounds(int rounds) {
        if (rounds < 1 || rounds > 3) {
            throw new IllegalArgumentException("조사 라운드는 1~3 사이여야 합니다.");
        }
        this.maxInvestigationRounds = rounds;
    }

    public void finishGame(GameRole winnerTeam) {
        this.phase = GamePhase.FINISHED;
        this.finishedAt = LocalDateTime.now();
        this.winnerTeam = winnerTeam;
    }

    public boolean checkWinCondition() {
        if (this.phase == GamePhase.FINISHED) {
            return true;
        }

        long criminalCount = getAliveCriminalCount();
        long citizenCount = getAliveCitizenCount();

        // 1. 범인이 모두 검거됨(사망) -> 시민 승리
        if (criminalCount == 0) {
            finishGame(GameRole.SUSPECT); // 시민(용의자/탐정) 팀 승리
            return true;
        }

        // 2. 범인 수 >= 시민 수 -> 범인 승리 (머더미스터리 규칙에 따라 다를 수 있음)
        if (criminalCount >= citizenCount) {
            finishGame(GameRole.CRIMINAL);
            return true;
        }

        return false;
    }

    private long getAliveCriminalCount() {
        return players.stream()
                .filter(p -> p.getIsAlive() && p.getRole() == GameRole.CRIMINAL)
                .count();
    }

    private long getAliveCitizenCount() {
        return players.stream()
                .filter(p -> p.getIsAlive() && p.getRole() != GameRole.CRIMINAL)
                .count();
    }
}
