package com.aenigma.ai.collector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 라운드 요약 엔티티
 * 
 * GM이 각 라운드의 음성 대화 내용을 텍스트로 요약한 것을 저장합니다.
 * AI 학습 시 음성 대화 내용을 보완하는 데이터로 사용됩니다.
 */
@Entity
@Table(name = "round_summaries", indexes = {
        @Index(name = "idx_round_summary_game", columnList = "game_id"),
        @Index(name = "idx_round_summary_game_round", columnList = "game_id, round")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoundSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "round", nullable = false)
    private int round;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "key_events", columnDefinition = "TEXT")
    private String keyEvents; // JSON: ["A가 B를 의심", "C가 알리바이 주장"]

    @Column(name = "suspicions", columnDefinition = "TEXT")
    private String suspicions; // JSON: {"A": ["B", "C"], "B": ["A"]} - 누가 누구를 의심했는지

    @Column(name = "recorded_by", length = 100)
    private String recordedBy; // GM 식별자

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === 팩토리 메서드 ===

    public static RoundSummary create(UUID gameId, int round, String phase,
            String summary, String recordedBy) {
        return RoundSummary.builder()
                .gameId(gameId)
                .round(round)
                .phase(phase)
                .summary(summary)
                .recordedBy(recordedBy)
                .build();
    }

    public void updateKeyEvents(String keyEvents) {
        this.keyEvents = keyEvents;
    }

    public void updateSuspicions(String suspicions) {
        this.suspicions = suspicions;
    }
}
