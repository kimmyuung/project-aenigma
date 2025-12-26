package com.aenigma.ai.collector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 세션 스냅샷
 * 
 * 게임의 전체 컨텍스트를 저장합니다.
 * AI 학습 시 각 이벤트가 발생한 시점의 게임 상태를 파악하는 데 사용됩니다.
 */
@Entity
@Table(name = "game_sessions", indexes = {
        @Index(name = "idx_session_game", columnList = "game_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false, unique = true)
    private UUID gameId;

    // === 시나리오 정보 ===

    @Column(name = "scenario_id", length = 100)
    private String scenarioId;

    @Column(name = "scenario_title", length = 200)
    private String scenarioTitle;

    @Column(name = "scenario_description", columnDefinition = "TEXT")
    private String scenarioDescription;

    @Column(name = "scenario_difficulty", length = 30)
    private String scenarioDifficulty;

    // === 게임 설정 ===

    @Column(name = "player_count")
    private int playerCount;

    @Column(name = "max_investigation_rounds")
    private int maxInvestigationRounds;

    // === 역할 배정 정보 (JSON) ===

    @Column(name = "role_assignments", columnDefinition = "TEXT")
    private String roleAssignments; // JSON: {"playerId": "role", ...}

    // === 단서 요약 (JSON) ===

    @Column(name = "public_clues", columnDefinition = "TEXT")
    private String publicClues; // JSON: [{title, content}, ...]

    @Column(name = "private_clues_summary", columnDefinition = "TEXT")
    private String privateCluesSummary; // JSON: {"playerId": [{title, content}], ...}

    // === 게임 결과 ===

    @Column(name = "winning_team", length = 50)
    private String winningTeam;

    @Column(name = "final_votes", columnDefinition = "TEXT")
    private String finalVotes; // JSON: 최종 투표 결과

    @Column(name = "game_duration_minutes")
    private Integer gameDurationMinutes;

    // === 메타데이터 ===

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === 업데이트 메서드 ===

    public void updateRoleAssignments(String roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public void updatePublicClues(String publicClues) {
        this.publicClues = publicClues;
    }

    public void updatePrivateCluesSummary(String privateCluesSummary) {
        this.privateCluesSummary = privateCluesSummary;
    }

    public void finishGame(String winningTeam, String finalVotes) {
        this.winningTeam = winningTeam;
        this.finalVotes = finalVotes;
        this.finishedAt = LocalDateTime.now();

        if (startedAt != null) {
            this.gameDurationMinutes = (int) java.time.Duration.between(startedAt, finishedAt).toMinutes();
        }
    }
}
