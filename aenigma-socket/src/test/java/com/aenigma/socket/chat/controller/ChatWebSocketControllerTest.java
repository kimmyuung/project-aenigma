package com.aenigma.socket.chat.controller;

import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.user.entity.User;
import com.aenigma.socket.chat.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    private UUID gameId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        senderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("채팅 메시지 전송 성공")
    void handleChatMessage() {
        // given
        ChatRequest request = new ChatRequest();
        request.setGameId(gameId);
        request.setSenderId(senderId);
        request.setContent("Hello World");
        request.setType(MessageType.PUBLIC);

        Game game = Game.builder().id(gameId).build();
        User user = User.builder().nickname("TestUser").build();
        GamePlayer sender = GamePlayer.builder().id(senderId).user(user).build();

        given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
        given(gamePlayerRepository.findById(senderId)).willReturn(Optional.of(sender));

        // when
        chatWebSocketController.handleChatMessage(request);

        // then
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/room/" + gameId), any(Object.class));
    }
}
