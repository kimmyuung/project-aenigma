package com.aenigma.socket.chat.controller;

import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.repository.ChatMessageRepository;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.user.entity.User;
import com.aenigma.socket.chat.dto.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 테스트")
class ChatWebSocketControllerTest {

        @InjectMocks
        private ChatWebSocketController chatWebSocketController;

        @Mock
        private SimpMessagingTemplate messagingTemplate;

        @Mock
        private GameRepository gameRepository;

        @Mock
        private GamePlayerRepository gamePlayerRepository;

        @Mock
        private ChatMessageRepository chatMessageRepository;

        private UUID gameId;
        private UUID senderId;
        private UUID receiverId;
        private Game game;
        private User senderUser;
        private User receiverUser;
        private GamePlayer sender;
        private GamePlayer receiver;

        @BeforeEach
        void setUp() {
                gameId = UUID.randomUUID();
                senderId = UUID.randomUUID();
                receiverId = UUID.randomUUID();

                senderUser = User.builder()
                                .id(UUID.randomUUID())
                                .nickname("TestUser")
                                .displayTag("1234")
                                .build();

                receiverUser = User.builder()
                                .id(UUID.randomUUID())
                                .nickname("ReceiverUser")
                                .displayTag("5678")
                                .build();

                game = Game.builder()
                                .id(gameId)
                                .players(new ArrayList<>())
                                .build();

                sender = GamePlayer.builder()
                                .id(senderId)
                                .user(senderUser)
                                .role(GameRole.SUSPECT)
                                .isAlive(true)
                                .build();

                receiver = GamePlayer.builder()
                                .id(receiverId)
                                .user(receiverUser)
                                .role(GameRole.DETECTIVE)
                                .isAlive(true)
                                .build();
        }

        @Nested
        @DisplayName("공개 메시지 처리")
        class PublicMessageTest {

                @Test
                @DisplayName("PUBLIC 메시지 전송 성공")
                void handleChatMessage() {
                        // given
                        ChatRequest request = new ChatRequest();
                        request.setGameId(gameId);
                        request.setSenderId(senderId);
                        request.setContent("Hello World");
                        request.setType(MessageType.PUBLIC);

                        given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
                        given(gamePlayerRepository.findById(senderId)).willReturn(Optional.of(sender));

                        // when
                        chatWebSocketController.handleChatMessage(request);

                        // then
                        verify(messagingTemplate).convertAndSend(eq("/sub/chat/room/" + gameId), any(Object.class));
                        verify(chatMessageRepository).save(any());
                }
        }

        @Nested
        @DisplayName("귓속말 메시지 처리")
        class WhisperMessageTest {

                @Test
                @DisplayName("WHISPER 메시지 전송 성공")
                void handleWhisperMessage() {
                        // given
                        ChatRequest request = new ChatRequest();
                        request.setGameId(gameId);
                        request.setSenderId(senderId);
                        request.setReceiverId(receiverId);
                        request.setContent("귓속말 테스트");
                        request.setType(MessageType.WHISPER);

                        given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
                        given(gamePlayerRepository.findById(senderId)).willReturn(Optional.of(sender));
                        given(gamePlayerRepository.findById(receiverId)).willReturn(Optional.of(receiver));

                        // when
                        chatWebSocketController.handleChatMessage(request);

                        // then
                        verify(messagingTemplate).convertAndSendToUser(eq(senderId.toString()), eq("/sub/chat/whisper"),
                                        any(Object.class));
                        verify(messagingTemplate).convertAndSendToUser(eq(receiverId.toString()),
                                        eq("/sub/chat/whisper"),
                                        any(Object.class));
                }

                @Test
                @DisplayName("수신자가 없는 귓속말은 예외 발생")
                void throwsExceptionWhenReceiverNotSpecified() {
                        // given
                        ChatRequest request = new ChatRequest();
                        request.setGameId(gameId);
                        request.setSenderId(senderId);
                        request.setReceiverId(null); // 수신자 없음
                        request.setContent("귓속말");
                        request.setType(MessageType.WHISPER);

                        given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
                        given(gamePlayerRepository.findById(senderId)).willReturn(Optional.of(sender));

                        // when & then
                        assertThatThrownBy(() -> chatWebSocketController.handleChatMessage(request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("귓속말 수신자가 지정되지 않았습니다");
                }
        }

        @Nested
        @DisplayName("예외 상황 처리")
        class ExceptionTest {

                @Test
                @DisplayName("게임을 찾을 수 없을 때 예외 발생")
                void throwsExceptionWhenGameNotFound() {
                        // given
                        ChatRequest request = new ChatRequest();
                        request.setGameId(gameId);
                        request.setSenderId(senderId);
                        request.setContent("Hello");
                        request.setType(MessageType.PUBLIC);

                        given(gameRepository.findById(gameId)).willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> chatWebSocketController.handleChatMessage(request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("게임을 찾을 수 없습니다");
                }

                @Test
                @DisplayName("플레이어를 찾을 수 없을 때 예외 발생")
                void throwsExceptionWhenPlayerNotFound() {
                        // given
                        ChatRequest request = new ChatRequest();
                        request.setGameId(gameId);
                        request.setSenderId(senderId);
                        request.setContent("Hello");
                        request.setType(MessageType.PUBLIC);

                        given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
                        given(gamePlayerRepository.findById(senderId)).willReturn(Optional.empty());

                        // when & then
                        assertThatThrownBy(() -> chatWebSocketController.handleChatMessage(request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("플레이어를 찾을 수 없습니다");
                }
        }
}
