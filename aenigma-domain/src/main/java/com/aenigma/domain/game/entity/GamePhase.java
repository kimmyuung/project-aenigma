package com.aenigma.domain.game.entity;

/**
 * 게임 진행 단계
 */
public enum GamePhase {
    /**
     * 게임 시작 및 역할 배정, 오프닝
     */
    INTRO,

    /**
     * 조사 및 대화 시간 (단서 확인, 밀담 등)
     */
    INVESTIGATION,

    /**
     * 최종 범인 지목 투표
     */
    FINAL_VOTE,

    /**
     * 사건의 전말 공개 및 결말
     */
    CONCLUSION,

    /**
     * 게임 종료 (방 대기 상태로 복귀)
     */
    FINISHED
}
