package com.aenigma.socket.chat.dto;

import com.aenigma.domain.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 채팅 메시지 요청 DTO
 * 
 * 클라이언트 -> 서버로 전송되는 채팅 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    /**
     * 게임 ID
     */
    private UUID gameId;

    /**
     * 발신자 ID (GamePlayer ID)
     */
    private UUID senderId;

    /**
     * 수신자 ID (귓속말인 경우에만 사용)
     */
    private UUID receiverId;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 메시지 유형
     */
    private MessageType type;
}
