package com.aenigma.socket.chat.controller;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.service.ChatService;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.socket.chat.dto.ChatRequest;
import com.aenigma.socket.chat.dto.ChatResponse;
import com.aenigma.socket.discord.service.DiscordChatSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 WebSocket Controller
 * 
 * 클라이언트는 /pub/chat/message로 메시지를 발행하고,
 * /sub/chat/room/{gameId}를 구독하여 메시지를 수신합니다.
 * 
 * ChatService를 통해 메시지를 저장하고, WebSocket을 통해 브로드캐스트합니다.
 * Discord 동기화 서비스를 통해 Discord로도 메시지를 전송합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

        private final SimpMessagingTemplate messagingTemplate;
        private final ChatService chatService;
        private final GameRepository gameRepository;
        private final GamePlayerRepository gamePlayerRepository;
        private final Optional<DiscordChatSyncService> discordChatSyncService;

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
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "게임을 찾을 수 없습니다: " + request.getGameId()));

                GamePlayer sender = gamePlayerRepository.findById(request.getSenderId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "플레이어를 찾을 수 없습니다: " + request.getSenderId()));

                // 메시지 유형에 따른 처리
                switch (request.getType()) {
                        case PUBLIC -> handlePublicMessage(game, sender, request);
                        case WHISPER -> handleWhisperMessage(game, sender, request);
                        case SYSTEM -> handleSystemMessage(game, request);
                        default -> log.warn("알 수 없는 메시지 유형: {}", request.getType());
                }
        }

        /**
         * 공개 메시지 처리 - 모든 참가자에게 브로드캐스트
         */
        private void handlePublicMessage(Game game, GamePlayer sender, ChatRequest request) {
                // ChatService를 통해 메시지 저장
                ChatMessage chatMessage = chatService.sendPublicMessage(
                                game.getId(), sender.getId(), request.getContent());

                // 응답 생성 및 브로드캐스트
                ChatResponse response = buildResponse(chatMessage, sender);
                String destination = "/sub/chat/room/" + game.getId();
                messagingTemplate.convertAndSend(destination, response);

                // Discord로도 전송
                discordChatSyncService.ifPresent(
                                service -> service.sendToDiscord(game.getId(), sender.getUser().getNickname(),
                                                request.getContent(), MessageType.PUBLIC));

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
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "수신자를 찾을 수 없습니다: " + request.getReceiverId()));

                // ChatService를 통해 메시지 저장
                ChatMessage chatMessage = chatService.sendWhisper(
                                game.getId(), sender.getId(), receiver.getId(), request.getContent());

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
         * 시스템 메시지 처리 - 모든 참가자에게 브로드캐스트
         */
        private void handleSystemMessage(Game game, ChatRequest request) {
                // ChatService를 통해 시스템 메시지 저장
                ChatMessage chatMessage = chatService.sendSystemMessage(game.getId(), request.getContent());

                // 응답 생성
                ChatResponse response = ChatResponse.builder()
                                .messageId(chatMessage.getId())
                                .gameId(game.getId())
                                .sender(null)
                                .content(chatMessage.getContent())
                                .type(MessageType.SYSTEM)
                                .timestamp(LocalDateTime.now())
                                .build();

                // 모든 참가자에게 브로드캐스트
                String destination = "/sub/chat/room/" + game.getId();
                messagingTemplate.convertAndSend(destination, response);

                log.debug("시스템 메시지 브로드캐스트: content={}", request.getContent());
        }

        /**
         * 외부에서 시스템 메시지 전송 (게임 이벤트용)
         */
        public void broadcastSystemMessage(UUID gameId, String content) {
                ChatMessage chatMessage = chatService.sendSystemMessage(gameId, content);

                ChatResponse response = ChatResponse.builder()
                                .messageId(chatMessage.getId())
                                .gameId(gameId)
                                .sender(null)
                                .content(content)
                                .type(MessageType.SYSTEM)
                                .timestamp(LocalDateTime.now())
                                .build();

                String destination = "/sub/chat/room/" + gameId;
                messagingTemplate.convertAndSend(destination, response);
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
