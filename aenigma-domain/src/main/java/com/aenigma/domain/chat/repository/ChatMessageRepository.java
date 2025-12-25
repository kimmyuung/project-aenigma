package com.aenigma.domain.chat.repository;

import com.aenigma.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * 게임의 채팅 기록 조회 (시간순)
     */
    List<ChatMessage> findByGameIdOrderByCreatedAtAsc(UUID gameId);
}
