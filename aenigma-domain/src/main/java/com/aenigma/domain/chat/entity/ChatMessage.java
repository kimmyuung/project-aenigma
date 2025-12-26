package com.aenigma.domain.chat.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 채팅 메시지 엔티티
 * 
 * 게임 내 채팅 메시지를 저장하고 관리합니다.
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_game", columnList = "game_id"),
        @Index(name = "idx_chat_sender", columnList = "sender_id"),
        @Index(name = "idx_chat_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 메시지가 속한 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 메시지 발신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private GamePlayer sender;

    /**
     * 귓속말 수신자 (WHISPER 타입인 경우에만 사용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private GamePlayer receiver;

    /**
     * 메시지 내용
     */
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    /**
     * 메시지 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type;

    // === Factory Methods ===

    public static ChatMessage createPublic(Game game, GamePlayer sender, String content) {
        return ChatMessage.builder()
                .game(game)
                .sender(sender)
                .content(content)
                .type(MessageType.PUBLIC)
                .build();
    }

    public static ChatMessage createWhisper(Game game, GamePlayer sender, GamePlayer receiver, String content) {
        return ChatMessage.builder()
                .game(game)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .type(MessageType.WHISPER)
                .build();
    }

    public static ChatMessage createSystem(Game game, String content) {
        return ChatMessage.builder()
                .game(game)
                .content(content)
                .type(MessageType.SYSTEM)
                .build();
    }
}
