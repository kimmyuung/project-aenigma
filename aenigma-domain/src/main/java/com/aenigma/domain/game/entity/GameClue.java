package com.aenigma.domain.game.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.scenario.entity.ClueType;
import com.aenigma.domain.scenario.entity.ScenarioClue;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 게임 내 단서 엔티티
 * 
 * 시나리오의 단서(ScenarioClue)가 게임에서 어떻게 사용되는지 관리합니다.
 * - 공개 여부
 * - 발견자
 * - 공개 시점
 */
@Entity
@Table(name = "game_clues", indexes = {
        @Index(name = "idx_game_clue_game", columnList = "game_id"),
        @Index(name = "idx_game_clue_discovered", columnList = "is_discovered")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameClue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 소속 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 원본 시나리오 단서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_clue_id", nullable = false)
    private ScenarioClue scenarioClue;

    /**
     * 단서 제목 (복사본 - 빠른 조회용)
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 단서 내용 (복사본)
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 단서 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "clue_type", nullable = false, length = 20)
    private ClueType clueType;

    /**
     * 배정된 플레이어 (개인 단서인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_player_id")
    private GamePlayer assignedPlayer;

    /**
     * 발견 여부
     */
    @Column(name = "is_discovered", nullable = false)
    @Builder.Default
    private Boolean isDiscovered = false;

    /**
     * 발견한 플레이어
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discovered_by_id")
    private GamePlayer discoveredBy;

    /**
     * 공개 라운드 (이 라운드부터 발견 가능)
     */
    private Integer revealRound;

    /**
     * 중요도 (1~5)
     */
    @Builder.Default
    private int importance = 3;

    /**
     * 이미지 URL
     */
    private String imageUrl;

    // === 비즈니스 메서드 ===

    /**
     * 단서 발견 처리
     */
    public void discover(GamePlayer player) {
        this.isDiscovered = true;
        this.discoveredBy = player;
    }

    /**
     * 현재 라운드에서 발견 가능한지 확인
     */
    public boolean isDiscoverable(int currentRound) {
        if (this.isDiscovered) {
            return false;
        }
        return this.revealRound == null || currentRound >= this.revealRound;
    }

    /**
     * 특정 플레이어가 볼 수 있는지 확인
     */
    public boolean isVisibleTo(GamePlayer player) {
        // 공개 단서는 발견되면 모두에게 보임
        if (this.clueType == ClueType.PUBLIC && this.isDiscovered) {
            return true;
        }
        // 개인 단서는 배정된 플레이어만
        if (this.clueType == ClueType.PERSONAL) {
            return this.assignedPlayer != null && this.assignedPlayer.equals(player);
        }
        // HIDDEN 단서는 발견자만
        if (this.clueType == ClueType.HIDDEN) {
            return this.isDiscovered && this.discoveredBy != null && this.discoveredBy.equals(player);
        }
        return false;
    }

    /**
     * ScenarioClue에서 GameClue 생성
     */
    public static GameClue from(ScenarioClue scenarioClue, Game game, GamePlayer assignedPlayer) {
        return GameClue.builder()
                .game(game)
                .scenarioClue(scenarioClue)
                .title(scenarioClue.getTitle())
                .content(scenarioClue.getContent())
                .clueType(scenarioClue.getClueType())
                .assignedPlayer(assignedPlayer)
                .revealRound(scenarioClue.getRevealRound())
                .importance(scenarioClue.getImportance())
                .imageUrl(scenarioClue.getImageUrl())
                .isDiscovered(scenarioClue.getClueType() == ClueType.PUBLIC)
                .build();
    }
}
