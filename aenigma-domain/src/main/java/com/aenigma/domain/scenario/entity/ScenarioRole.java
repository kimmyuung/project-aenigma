package com.aenigma.domain.scenario.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.game.entity.GameRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 시나리오 역할 엔티티
 * 
 * 시나리오에서 플레이어가 맡는 캐릭터/역할을 정의합니다.
 */
@Entity
@Table(name = "scenario_roles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScenarioRole extends BaseTimeEntity {

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
     * 역할 이름 (예: "정 변호사", "김 형사")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 게임 역할 유형 (범인/탐정/용의자)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameRole roleType;

    /**
     * 캐릭터 설명 (플레이어에게 전달됨)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 비밀 정보 (이 역할만 아는 정보)
     */
    @Column(columnDefinition = "TEXT")
    private String secretInfo;

    /**
     * 캐릭터 목표/동기
     */
    @Column(columnDefinition = "TEXT")
    private String objective;

    /**
     * 관계 정보 (다른 캐릭터와의 관계)
     */
    @Column(columnDefinition = "TEXT")
    private String relationships;

    /**
     * 아이콘/이미지 URL
     */
    private String imageUrl;

    /**
     * 정렬 순서
     */
    @Builder.Default
    private int displayOrder = 0;

    /**
     * 이 역할에게 주어지는 개인 단서 목록
     */
    @OneToMany(mappedBy = "assignedRole", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScenarioClue> personalClues = new ArrayList<>();

    // === 비즈니스 메서드 ===

    public boolean isCriminal() {
        return this.roleType == GameRole.CRIMINAL;
    }

    public boolean isDetective() {
        return this.roleType == GameRole.DETECTIVE;
    }
}
