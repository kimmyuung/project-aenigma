package com.aenigma.domain.scenario.entity;

/**
 * 시나리오 공개 상태
 */
public enum ScenarioStatus {
    DRAFT, // 작성 중 (비공개)
    PUBLISHED, // 공개됨 (마켓에 노출)
    ARCHIVED, // 보관됨 (마켓에서 숨김)
    REJECTED // 검토 거절 (정책 위반 등)
}
