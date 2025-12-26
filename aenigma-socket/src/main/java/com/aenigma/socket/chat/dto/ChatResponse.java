package com.aenigma.socket.chat.dto;

import com.aenigma.domain.chat.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 응답 DTO
 * 
 * 서버 -> 클라이언트로 브로드캐스트되는 채팅 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /**
     * 메시지 ID
     */
    private UUID messageId;

    /**
     * 게임 ID
     */
    private UUID gameId;

    /**
     * 발신자 정보
     */
    private SenderInfo sender;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 메시지 유형
     */
    private MessageType type;

    /**
     * 전송 시간
     */
    private LocalDateTime timestamp;

    /**
     * 발신자 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SenderInfo {
        private UUID playerId;
        private String nickname;
        private String displayTag;

        public String getDisplayName() {
            if (displayTag == null || displayTag.isBlank()) {
                return nickname;
            }
            return nickname + "#" + displayTag;
        }
    }
}
