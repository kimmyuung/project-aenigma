package com.aenigma.domain.chat.service;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.repository.ChatMessageRepository;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 서비스
 * 
 * 게임 내 채팅 메시지 생성 및 조회를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final ChatMessageRepository chatMessageRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 공개 메시지 전송
     */
    @Transactional
    public ChatMessage sendPublicMessage(UUID gameId, UUID senderId, String content) {
        validateMessageContent(content);

        Game game = findGameById(gameId);
        GamePlayer sender = findPlayerById(senderId);

        validatePlayerInGame(game, sender);
        validatePlayerAlive(sender);

        ChatMessage message = ChatMessage.createPublic(game, sender, content);
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("공개 메시지 전송: game={}, sender={}", gameId, sender.getUser().getNickname());
        return saved;
    }

    /**
     * 귓속말 전송
     */
    @Transactional
    public ChatMessage sendWhisper(UUID gameId, UUID senderId, UUID receiverId, String content) {
        validateMessageContent(content);

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("자신에게 귓속말을 보낼 수 없습니다.");
        }

        Game game = findGameById(gameId);
        GamePlayer sender = findPlayerById(senderId);
        GamePlayer receiver = findPlayerById(receiverId);

        validatePlayerInGame(game, sender);
        validatePlayerInGame(game, receiver);
        validatePlayerAlive(sender);

        ChatMessage message = ChatMessage.createWhisper(game, sender, receiver, content);
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("귓속말 전송: game={}, sender={}, receiver={}",
                gameId, sender.getUser().getNickname(), receiver.getUser().getNickname());
        return saved;
    }

    /**
     * 시스템 메시지 전송
     */
    @Transactional
    public ChatMessage sendSystemMessage(UUID gameId, String content) {
        Game game = findGameById(gameId);

        ChatMessage message = ChatMessage.createSystem(game, content);
        ChatMessage saved = chatMessageRepository.save(message);

        log.info("시스템 메시지 전송: game={}, content={}", gameId, content);
        return saved;
    }

    /**
     * 게임의 모든 공개 메시지 조회
     */
    public List<ChatMessage> getPublicMessages(UUID gameId) {
        return chatMessageRepository.findByGameIdAndTypeOrderByCreatedAtAsc(gameId, MessageType.PUBLIC);
    }

    /**
     * 게임의 최근 메시지 조회
     */
    public List<ChatMessage> getRecentMessages(UUID gameId, int limit) {
        if (limit <= 0 || limit > 100) {
            limit = 50; // 기본값
        }
        return chatMessageRepository.findRecentMessages(gameId, limit);
    }

    /**
     * 플레이어의 귓속말 조회
     */
    public List<ChatMessage> getWhisperMessages(UUID gameId, UUID playerId) {
        return chatMessageRepository.findWhisperMessages(gameId, playerId);
    }

    /**
     * 게임의 시스템 메시지 조회
     */
    public List<ChatMessage> getSystemMessages(UUID gameId) {
        return chatMessageRepository.findByGameIdAndTypeOrderByCreatedAtAsc(gameId, MessageType.SYSTEM);
    }

    // === Private Helper Methods ===

    private Game findGameById(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));
    }

    private GamePlayer findPlayerById(UUID playerId) {
        return gamePlayerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));
    }

    private void validateMessageContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용이 비어있습니다.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("메시지가 너무 깁니다. (최대 " + MAX_MESSAGE_LENGTH + "자)");
        }
    }

    private void validatePlayerInGame(Game game, GamePlayer player) {
        boolean isInGame = game.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(player.getId()));
        if (!isInGame) {
            throw new IllegalStateException("해당 게임에 참여하지 않은 플레이어입니다.");
        }
    }

    private void validatePlayerAlive(GamePlayer player) {
        if (!player.getIsAlive()) {
            throw new IllegalStateException("사망한 플레이어는 메시지를 보낼 수 없습니다.");
        }
    }
}
