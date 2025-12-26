package com.aenigma.domain.scenario.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 시나리오 단서 엔티티
 * 
 * 게임에서 사용되는 단서(증거, 정보)를 정의합니다.
 */
@Entity
@Table(name = "scenario_clues")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScenarioClue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 소속 시나리오
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    /**
     * 단서 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 단서 내용
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 단서 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClueType clueType = ClueType.PUBLIC;

    /**
     * 배정된 역할 (개인 단서인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_role_id")
    private ScenarioRole assignedRole;

    /**
     * 공개 페이즈 (어느 페이즈에서 공개되는지)
     */
    private String revealPhase;

    /**
     * 공개 라운드 (조사 N라운드에 공개)
     */
    private Integer revealRound;

    /**
     * 이미지 URL (증거 사진 등)
     */
    private String imageUrl;

    /**
     * 중요도 (1~5, 높을수록 중요한 단서)
     */
    @Builder.Default
    private int importance = 3;

    /**
     * 정렬 순서
     */
    @Builder.Default
    private int displayOrder = 0;

    // === 비즈니스 메서드 ===

    public boolean isPersonal() {
        return this.clueType == ClueType.PERSONAL && this.assignedRole != null;
    }

    public boolean isPublic() {
        return this.clueType == ClueType.PUBLIC;
    }

    public boolean isHidden() {
        return this.clueType == ClueType.HIDDEN;
    }
}
