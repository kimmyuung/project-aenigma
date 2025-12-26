package com.aenigma.domain.chat.entity;

/**
 * 채팅 메시지 유형
 */
public enum MessageType {

    /**
     * 공개 채팅 (모든 참가자에게 전송)
     */
    PUBLIC,

    /**
     * 귓속말 (특정 플레이어에게만 전송)
     */
    WHISPER,

    /**
     * 시스템 메시지 (게임 이벤트 알림)
     */
    SYSTEM
}
