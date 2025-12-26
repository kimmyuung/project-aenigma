package com.aenigma.domain.scenario.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 시나리오 리뷰
 */
@Entity
@Table(name = "scenario_reviews", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "scenario_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScenarioReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reviewer;

    /**
     * 리뷰 대상 시나리오
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    /**
     * 평점 (1~5)
     */
    @Column(nullable = false)
    private int rating;

    /**
     * 리뷰 내용
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 스포일러 포함 여부
     */
    @Builder.Default
    private boolean hasSpoiler = false;

    /**
     * 좋아요 수
     */
    @Builder.Default
    private int likeCount = 0;

    // === 비즈니스 메서드 ===

    public void updateReview(int rating, String content, boolean hasSpoiler) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
        this.rating = rating;
        this.content = content;
        this.hasSpoiler = hasSpoiler;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
