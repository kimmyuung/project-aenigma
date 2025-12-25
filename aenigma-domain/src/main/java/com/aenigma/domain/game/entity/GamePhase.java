package com.aenigma.domain.game.entity;

/**
 * 게임 진행 단계
 */
public enum GamePhase {
    /**
     * 게임 준비 중 (역할 배정 등)
     */
    PREPARING,

    /**
     * 낮 시간 - 토론 및 추리
     */
    DAY,

    /**
     * 투표 시간 - 용의자 지목
     */
    VOTING,

    /**
     * 밤 시간 - 범인 행동
     */
    NIGHT,

    /**
     * 게임 종료
     */
    FINISHED
}
