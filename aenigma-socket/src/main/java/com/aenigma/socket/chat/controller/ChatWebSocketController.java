package com.aenigma.socket.chat.controller;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.repository.ChatMessageRepository;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.socket.chat.dto.ChatRequest;
import com.aenigma.socket.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 채팅 WebSocket Controller
 * 
 * 클라이언트는 /pub/chat/message로 메시지를 발행하고,
 * /sub/chat/room/{gameId}를 구독하여 메시지를 수신합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 채팅 메시지 처리
     * 
     * 클라이언트 발행: /pub/chat/message
     * 브로드캐스트: /sub/chat/room/{gameId}
     */
    @MessageMapping("/chat/message")
    public void handleChatMessage(ChatRequest request) {
        log.debug("채팅 메시지 수신: gameId={}, senderId={}, type={}",
                request.getGameId(), request.getSenderId(), request.getType());

        // 게임 및 발신자 조회
        Game game = gameRepository.findById(request.getGameId())
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: " + request.getGameId()));

        GamePlayer sender = gamePlayerRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다: " + request.getSenderId()));

        // 메시지 유형에 따른 처리
        switch (request.getType()) {
            case PUBLIC -> handlePublicMessage(game, sender, request);
            case WHISPER -> handleWhisperMessage(game, sender, request);
            case CRIMINAL_ONLY -> handleCriminalMessage(game, sender, request);
            default -> log.warn("알 수 없는 메시지 유형: {}", request.getType());
        }
    }

    /**
     * 공개 메시지 처리 - 모든 참가자에게 브로드캐스트
     */
    private void handlePublicMessage(Game game, GamePlayer sender, ChatRequest request) {
        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.createPublic(game, sender, request.getContent());
        chatMessageRepository.save(chatMessage);

        // 응답 생성
        ChatResponse response = buildResponse(chatMessage, sender);

        // 모든 참가자에게 브로드캐스트
        String destination = "/sub/chat/room/" + game.getId();
        messagingTemplate.convertAndSend(destination, response);

        log.debug("공개 메시지 브로드캐스트: destination={}", destination);
    }

    /**
     * 귓속말 처리 - 발신자와 수신자에게만 전송
     */
    private void handleWhisperMessage(Game game, GamePlayer sender, ChatRequest request) {
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("귓속말 수신자가 지정되지 않았습니다.");
        }

        GamePlayer receiver = gamePlayerRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다: " + request.getReceiverId()));

        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.createWhisper(game, sender, receiver, request.getContent());
        chatMessageRepository.save(chatMessage);

        // 응답 생성
        ChatResponse response = buildResponse(chatMessage, sender);

        // 발신자와 수신자에게만 전송
        messagingTemplate.convertAndSendToUser(sender.getId().toString(),
                "/sub/chat/whisper", response);
        messagingTemplate.convertAndSendToUser(receiver.getId().toString(),
                "/sub/chat/whisper", response);

        log.debug("귓속말 전송: sender={}, receiver={}", sender.getId(), receiver.getId());
    }

    /**
     * 범인 팀 전용 메시지 처리 - 범인들에게만 전송
     */
    private void handleCriminalMessage(Game game, GamePlayer sender, ChatRequest request) {
        // 발신자가 범인인지 확인
        if (sender.getRole() != GameRole.CRIMINAL) {
            log.warn("범인이 아닌 플레이어가 범인 채팅 시도: playerId={}", sender.getId());
            return;
        }

        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .game(game)
                .sender(sender)
                .content(request.getContent())
                .type(MessageType.CRIMINAL_ONLY)
                .build();
        chatMessageRepository.save(chatMessage);

        // 응답 생성
        ChatResponse response = buildResponse(chatMessage, sender);

        // 게임 내 모든 범인에게 전송
        List<GamePlayer> criminals = game.getPlayers().stream()
                .filter(p -> p.getRole() == GameRole.CRIMINAL)
                .toList();

        for (GamePlayer criminal : criminals) {
            messagingTemplate.convertAndSendToUser(criminal.getId().toString(),
                    "/sub/chat/criminal", response);
        }

        log.debug("범인 채팅 전송: criminalCount={}", criminals.size());
    }

    /**
     * ChatResponse 빌드
     */
    private ChatResponse buildResponse(ChatMessage message, GamePlayer sender) {
        ChatResponse.SenderInfo senderInfo = null;

        if (sender != null && sender.getUser() != null) {
            senderInfo = ChatResponse.SenderInfo.builder()
                    .playerId(sender.getId())
                    .nickname(sender.getUser().getNickname())
                    .displayTag(sender.getUser().getDisplayTag())
                    .build();
        }

        return ChatResponse.builder()
                .messageId(message.getId())
                .gameId(message.getGame().getId())
                .sender(senderInfo)
                .content(message.getContent())
                .type(message.getType())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
