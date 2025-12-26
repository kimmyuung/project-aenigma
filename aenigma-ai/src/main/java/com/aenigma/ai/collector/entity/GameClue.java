package com.aenigma.ai.collector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 단서 엔티티
 * 
 * 각 플레이어가 보유한 개별 단서와 공개된 전체 단서를 저장합니다.
 * AI 학습 시 플레이어가 어떤 정보를 가지고 행동했는지 파악하는 데 사용됩니다.
 */
@Entity
@Table(name = "game_clues", indexes = {
        @Index(name = "idx_clue_game", columnList = "game_id"),
        @Index(name = "idx_clue_player", columnList = "player_id"),
        @Index(name = "idx_clue_type", columnList = "clue_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameClue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "scenario_id", length = 100)
    private String scenarioId;

    @Column(name = "scenario_title", length = 200)
    private String scenarioTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "clue_type", nullable = false, length = 30)
    private ClueType clueType;

    @Column(name = "player_id")
    private UUID playerId; // null이면 공개 단서

    @Column(name = "player_role", length = 50)
    private String playerRole;

    @Column(name = "clue_title", length = 200)
    private String clueTitle;

    @Column(name = "clue_content", columnDefinition = "TEXT")
    private String clueContent;

    @Column(name = "revealed_phase", length = 50)
    private String revealedPhase; // 단서가 공개된 페이즈

    @Column(name = "revealed_round")
    private Integer revealedRound; // 단서가 공개된 라운드

    @Column(name = "is_public", nullable = false)
    private boolean isPublic; // 공개 단서 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === Factory Methods ===

    /**
     * 개별 단서 생성 (플레이어 전용)
     */
    public static GameClue createPrivateClue(UUID gameId, String scenarioId, String scenarioTitle,
            UUID playerId, String playerRole,
            String clueTitle, String clueContent) {
        return GameClue.builder()
                .gameId(gameId)
                .scenarioId(scenarioId)
                .scenarioTitle(scenarioTitle)
                .clueType(ClueType.PRIVATE)
                .playerId(playerId)
                .playerRole(playerRole)
                .clueTitle(clueTitle)
                .clueContent(clueContent)
                .isPublic(false)
                .build();
    }

    /**
     * 공개 단서 생성 (모든 플레이어 공유)
     */
    public static GameClue createPublicClue(UUID gameId, String scenarioId, String scenarioTitle,
            String clueTitle, String clueContent,
            String revealedPhase, Integer revealedRound) {
        return GameClue.builder()
                .gameId(gameId)
                .scenarioId(scenarioId)
                .scenarioTitle(scenarioTitle)
                .clueType(ClueType.PUBLIC)
                .clueTitle(clueTitle)
                .clueContent(clueContent)
                .revealedPhase(revealedPhase)
                .revealedRound(revealedRound)
                .isPublic(true)
                .build();
    }

    /**
     * 역할 기반 단서 (특정 역할만 아는 정보)
     */
    public static GameClue createRoleClue(UUID gameId, String scenarioId, String scenarioTitle,
            UUID playerId, String playerRole,
            String clueTitle, String clueContent) {
        return GameClue.builder()
                .gameId(gameId)
                .scenarioId(scenarioId)
                .scenarioTitle(scenarioTitle)
                .clueType(ClueType.ROLE_SPECIFIC)
                .playerId(playerId)
                .playerRole(playerRole)
                .clueTitle(clueTitle)
                .clueContent(clueContent)
                .isPublic(false)
                .build();
    }

    /**
     * 단서 유형
     */
    public enum ClueType {
        PRIVATE, // 개별 단서 (해당 플레이어만 보유)
        PUBLIC, // 공개 단서 (모든 플레이어 공유)
        ROLE_SPECIFIC, // 역할 기반 단서 (역할에 따라 제공)
        DISCOVERED // 게임 중 발견된 단서
    }
}
