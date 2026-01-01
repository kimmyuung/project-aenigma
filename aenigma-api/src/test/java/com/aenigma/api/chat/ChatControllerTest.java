package com.aenigma.api.chat;

import com.aenigma.api.chat.controller.ChatController;
import com.aenigma.api.chat.dto.SendChatRequest;
import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.service.ChatService;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController 테스트")
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;
    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @InjectMocks
    private ChatController chatController;

    private ObjectMapper objectMapper;
    private UUID gameId;
    private UUID userId;
    private UUID playerId;
    private GamePlayer player;
    private User user;
    private Game game;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();

        gameId = UUID.randomUUID();
        userId = UUID.randomUUID();
        playerId = UUID.randomUUID();

        user = User.builder()
                .username("GUEST_testuser")
                .nickname("TestUser")
                .build();
        setId(user, userId);

        User host = User.builder()
                .username("GUEST_host")
                .nickname("Host")
                .build();

        Room room = Room.builder()
                .roomCode("ABC123")
                .title("Test Room")
                .host(host)
                .build();

        game = Game.builder()
                .room(room)
                .roundNumber(1)
                .build();
        setId(game, gameId);

        player = GamePlayer.builder()
                .game(game)
                .user(user)
                .role(GameRole.SUSPECT)
                .build();
        setId(player, playerId);
    }

    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/chats")
    class GetPublicMessagesTest {

        @Test
        @DisplayName("공개 메시지 목록 조회 성공")
        void success() throws Exception {
            // given
            ChatMessage message = ChatMessage.builder()
                    .game(game)
                    .sender(player)
                    .content("안녕하세요")
                    .messageType(MessageType.PUBLIC)
                    .build();

            given(chatService.getPublicMessages(gameId)).willReturn(List.of(message));

            // when & then
            mockMvc.perform(get("/api/games/{gameId}/chats", gameId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/chats/recent")
    class GetRecentMessagesTest {

        @Test
        @DisplayName("최근 메시지 조회 성공")
        void success() throws Exception {
            // given
            given(chatService.getRecentMessages(eq(gameId), anyInt())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/games/{gameId}/chats/recent", gameId)
                    .header("X-User-Id", userId.toString())
                    .param("limit", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/chats/whispers")
    class GetWhisperMessagesTest {

        @Test
        @DisplayName("귓속말 조회 성공")
        void success() throws Exception {
            // given
            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId))
                    .willReturn(Optional.of(player));
            given(chatService.getWhisperMessages(gameId, playerId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/games/{gameId}/chats/whispers", gameId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("게임 참여하지 않은 사용자 요청 시 실패")
        void failWhenNotParticipant() throws Exception {
            // given
            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId))
                    .willReturn(Optional.empty());

            // when & then
            mockMvc.perform(get("/api/games/{gameId}/chats/whispers", gameId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/games/{gameId}/chats/system")
    class GetSystemMessagesTest {

        @Test
        @DisplayName("시스템 메시지 조회 성공")
        void success() throws Exception {
            // given
            given(chatService.getSystemMessages(gameId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/games/{gameId}/chats/system", gameId)
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/games/{gameId}/chats")
    class SendMessageTest {

        @Test
        @DisplayName("공개 메시지 전송 성공")
        void sendPublicMessage() throws Exception {
            // given
            SendChatRequest request = new SendChatRequest();
            request.setContent("테스트 메시지");
            request.setWhisper(false);

            ChatMessage savedMessage = ChatMessage.builder()
                    .game(game)
                    .sender(player)
                    .content("테스트 메시지")
                    .messageType(MessageType.PUBLIC)
                    .build();

            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId))
                    .willReturn(Optional.of(player));
            given(chatService.sendPublicMessage(eq(gameId), eq(playerId), anyString()))
                    .willReturn(savedMessage);

            // when & then
            mockMvc.perform(post("/api/games/{gameId}/chats", gameId)
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("귓속말 전송 성공")
        void sendWhisperMessage() throws Exception {
            // given
            UUID receiverId = UUID.randomUUID();
            SendChatRequest request = new SendChatRequest();
            request.setContent("비밀 메시지");
            request.setWhisper(true);
            request.setReceiverId(receiverId);

            ChatMessage savedMessage = ChatMessage.builder()
                    .game(game)
                    .sender(player)
                    .content("비밀 메시지")
                    .messageType(MessageType.WHISPER)
                    .build();

            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId))
                    .willReturn(Optional.of(player));
            given(chatService.sendWhisper(eq(gameId), eq(playerId), eq(receiverId), anyString()))
                    .willReturn(savedMessage);

            // when & then
            mockMvc.perform(post("/api/games/{gameId}/chats", gameId)
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }
}
