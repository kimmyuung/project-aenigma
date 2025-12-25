package com.aenigma.domain.chat.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 게임 내 채팅 메시지
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_game", columnList = "game_id"),
        @Index(name = "idx_chat_created_at", columnList = "created_at")
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
     * 해당 채팅이 발생한 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 메시지 보낸 사람 (시스템 메시지인 경우 null 일 수 있음)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    /**
     * 메시지 내용
     */
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /**
     * 메시지 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ChatMessageType type;

    public enum ChatMessageType {
        PUBLIC, // 전체 채팅
        WHISPER, // 귓속말/밀담
        SYSTEM, // 시스템 알림
        NOTICE // 공지사항
    }
}
