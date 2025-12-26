package com.aenigma.api.chat.controller;

import com.aenigma.api.chat.dto.ChatMessageResponse;
import com.aenigma.api.chat.dto.SendChatRequest;
import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.service.ChatService;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 API Controller
 * 
 * 게임 내 채팅 메시지 전송 및 조회 API를 제공합니다.
 * 주로 히스토리 조회용이며, 실시간 채팅은 WebSocket을 통해 처리됩니다.
 */
@Tag(name = "Chat", description = "게임 채팅 API")
@RestController
@RequestMapping("/api/games/{gameId}/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 채팅 메시지 전송 (REST API 대안)
     */
    @Operation(summary = "메시지 전송", description = "게임에 채팅 메시지를 전송합니다.")
    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId,
            @Valid @RequestBody SendChatRequest request) {

        // 사용자의 GamePlayer 찾기
        GamePlayer player = findPlayerByGameAndUser(gameId, userId);

        ChatMessage message;
        if (request.isWhisper() && request.getReceiverId() != null) {
            message = chatService.sendWhisper(gameId, player.getId(), request.getReceiverId(), request.getContent());
        } else {
            message = chatService.sendPublicMessage(gameId, player.getId(), request.getContent());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ChatMessageResponse.from(message));
    }

    /**
     * 공개 메시지 조회
     */
    @Operation(summary = "공개 메시지 조회", description = "게임의 모든 공개 메시지를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getPublicMessages(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId) {

        List<ChatMessage> messages = chatService.getPublicMessages(gameId);
        List<ChatMessageResponse> response = messages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 메시지 조회
     */
    @Operation(summary = "최근 메시지 조회", description = "게임의 최근 N개 메시지를 조회합니다.")
    @GetMapping("/recent")
    public ResponseEntity<List<ChatMessageResponse>> getRecentMessages(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId,
            @RequestParam(defaultValue = "50") int limit) {

        List<ChatMessage> messages = chatService.getRecentMessages(gameId, limit);
        List<ChatMessageResponse> response = messages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 귓속말 조회
     */
    @Operation(summary = "귓속말 조회", description = "현재 사용자가 주고받은 귓속말을 조회합니다.")
    @GetMapping("/whispers")
    public ResponseEntity<List<ChatMessageResponse>> getWhisperMessages(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId) {

        // 사용자의 GamePlayer 찾기
        GamePlayer player = findPlayerByGameAndUser(gameId, userId);

        List<ChatMessage> messages = chatService.getWhisperMessages(gameId, player.getId());
        List<ChatMessageResponse> response = messages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 메시지 조회
     */
    @Operation(summary = "시스템 메시지 조회", description = "게임의 시스템 메시지를 조회합니다.")
    @GetMapping("/system")
    public ResponseEntity<List<ChatMessageResponse>> getSystemMessages(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId) {

        List<ChatMessage> messages = chatService.getSystemMessages(gameId);
        List<ChatMessageResponse> response = messages.stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // === Private Helper ===

    private GamePlayer findPlayerByGameAndUser(UUID gameId, UUID userId) {
        return gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("게임에 참여하지 않은 사용자입니다."));
    }
}
