package com.aenigma.domain.scenario.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 시나리오 엔티티
 * 
 * 머더 미스터리 시나리오의 기본 정보를 저장합니다.
 * 마켓플레이스에서 공유/판매될 수 있습니다.
 */
@Entity
@Table(name = "scenarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Scenario extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 시나리오 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 시나리오 설명 (마켓플레이스 노출용)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 시나리오 배경 스토리 (INTRO에서 공개)
     */
    @Column(columnDefinition = "TEXT")
    private String backgroundStory;

    /**
     * 사건의 전말 (게임 종료 후 공개)
     * 진범의 동기, 사건 경위, 단서의 의미 등을 포함합니다.
     */
    @Column(columnDefinition = "TEXT")
    private String caseSummary;

    /**
     * 작가 (시나리오 제작자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 권장 플레이어 수 (최소)
     */
    @Column(nullable = false)
    private int minPlayers;

    /**
     * 권장 플레이어 수 (최대)
     */
    @Column(nullable = false)
    private int maxPlayers;

    /**
     * 예상 플레이 시간 (분)
     */
    private int estimatedMinutes;

    /**
     * 난이도 (1~5)
     */
    @Column(nullable = false)
    @Builder.Default
    private int difficulty = 3;

    /**
     * 장르/태그
     */
    @ElementCollection
    @CollectionTable(name = "scenario_tags", joinColumns = @JoinColumn(name = "scenario_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * 가격 (0이면 무료)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    /**
     * 공개 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScenarioStatus status = ScenarioStatus.DRAFT;

    /**
     * 썸네일 이미지 URL
     */
    private String thumbnailUrl;

    /**
     * 조회수
     */
    @Builder.Default
    private long viewCount = 0;

    /**
     * 판매/다운로드 수
     */
    @Builder.Default
    private long downloadCount = 0;

    /**
     * 평균 평점 (1.0 ~ 5.0)
     */
    @Builder.Default
    private double averageRating = 0.0;

    /**
     * 리뷰 수
     */
    @Builder.Default
    private int reviewCount = 0;

    /**
     * 탐정 역할 포함 여부
     */
    @Builder.Default
    private boolean hasDetective = false;

    /**
     * 권장 조사 라운드 수
     */
    @Builder.Default
    private int recommendedRounds = 2;

    /**
     * 역할 목록
     */
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScenarioRole> roles = new ArrayList<>();

    /**
     * 단서 목록
     */
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScenarioClue> clues = new ArrayList<>();

    // === 비즈니스 메서드 ===

    public void publish() {
        if (this.roles.isEmpty()) {
            throw new IllegalStateException("역할이 없는 시나리오는 공개할 수 없습니다.");
        }
        this.status = ScenarioStatus.PUBLISHED;
    }

    public void unpublish() {
        this.status = ScenarioStatus.DRAFT;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public void updateRating(double newRating) {
        double totalRating = this.averageRating * this.reviewCount;
        this.reviewCount++;
        this.averageRating = (totalRating + newRating) / this.reviewCount;
    }

    public boolean isFree() {
        return this.price.compareTo(BigDecimal.ZERO) == 0;
    }

    public void addRole(ScenarioRole role) {
        this.roles.add(role);
        role.setScenario(this);
    }

    public void addClue(ScenarioClue clue) {
        this.clues.add(clue);
        clue.setScenario(this);
    }
}
