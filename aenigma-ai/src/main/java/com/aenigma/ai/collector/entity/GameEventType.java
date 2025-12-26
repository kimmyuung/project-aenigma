package com.aenigma.ai.collector.entity;

/**
 * 게임 이벤트 유형
 */
public enum GameEventType {
    // 채팅 관련
    CHAT_MESSAGE, // 공개 채팅
    WHISPER, // 귓속말/밀담
    SYSTEM_MESSAGE, // 시스템 메시지

    // 게임 진행
    GAME_START, // 게임 시작
    GAME_END, // 게임 종료
    PHASE_CHANGE, // 페이즈 전환
    ROUND_CHANGE, // 라운드 전환

    // 플레이어 행동
    VOTE_CAST, // 투표
    PLAYER_ELIMINATED, // 플레이어 탈락
    ROLE_ASSIGNED, // 역할 배정

    // 단서
    CLUE_DISCOVERED // 단서 발견
}
