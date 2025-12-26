package com.aenigma.domain.chat.repository;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 메시지 Repository
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * 게임의 모든 공개 메시지 조회
     */
    List<ChatMessage> findByGameIdAndTypeOrderByCreatedAtAsc(UUID gameId, MessageType type);

    /**
     * 게임의 최근 N개 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.game.id = :gameId ORDER BY cm.createdAt DESC LIMIT :limit")
    List<ChatMessage> findRecentMessages(@Param("gameId") UUID gameId, @Param("limit") int limit);

    /**
     * 특정 플레이어가 보내거나 받은 귓속말 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.game.id = :gameId AND cm.type = 'WHISPER' " +
            "AND (cm.sender.id = :playerId OR cm.receiver.id = :playerId) ORDER BY cm.createdAt ASC")
    List<ChatMessage> findWhisperMessages(@Param("gameId") UUID gameId, @Param("playerId") UUID playerId);
}
