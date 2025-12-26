package com.aenigma.api.chat.dto;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 응답 DTO
 */
@Getter
@Builder
public class ChatMessageResponse {

    private UUID id;
    private UUID gameId;
    private MessageType type;
    private String content;
    private SenderInfo sender;
    private SenderInfo receiver;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class SenderInfo {
        private UUID playerId;
        private UUID userId;
        private String nickname;
    }

    public static ChatMessageResponse from(ChatMessage message) {
        ChatMessageResponseBuilder builder = ChatMessageResponse.builder()
                .id(message.getId())
                .gameId(message.getGame().getId())
                .type(message.getType())
                .content(message.getContent())
                .createdAt(message.getCreatedAt());

        if (message.getSender() != null) {
            builder.sender(SenderInfo.builder()
                    .playerId(message.getSender().getId())
                    .userId(message.getSender().getUser().getId())
                    .nickname(message.getSender().getUser().getNickname())
                    .build());
        }

        if (message.getReceiver() != null) {
            builder.receiver(SenderInfo.builder()
                    .playerId(message.getReceiver().getId())
                    .userId(message.getReceiver().getUser().getId())
                    .nickname(message.getReceiver().getUser().getNickname())
                    .build());
        }

        return builder.build();
    }
}
