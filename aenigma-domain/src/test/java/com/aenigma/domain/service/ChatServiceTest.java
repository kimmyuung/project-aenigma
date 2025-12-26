package com.aenigma.domain.service;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.chat.repository.ChatMessageRepository;
import com.aenigma.domain.chat.service.ChatService;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @InjectMocks
    private ChatService chatService;

    private Game game;
    private GamePlayer sender;
    private GamePlayer receiver;
    private User senderUser;
    private User receiverUser;

    @BeforeEach
    void setUp() {
        // User 생성
        senderUser = User.builder()
                .id(UUID.randomUUID())
                .username("sender_user")
                .nickname("발신자")
                .build();

        receiverUser = User.builder()
                .id(UUID.randomUUID())
                .username("receiver_user")
                .nickname("수신자")
                .build();

        // Room 생성
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .title("테스트 방")
                .build();

        // Game 생성
        game = Game.builder()
                .id(UUID.randomUUID())
                .room(room)
                .roundNumber(1)
                .phase(GamePhase.INVESTIGATION)
                .players(new ArrayList<>())
                .build();

        // GamePlayer 생성
        sender = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(senderUser)
                .role(GameRole.DETECTIVE)
                .isAlive(true)
                .build();

        receiver = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(receiverUser)
                .role(GameRole.SUSPECT)
                .isAlive(true)
                .build();

        game.getPlayers().add(sender);
        game.getPlayers().add(receiver);
    }

    @Nested
    @DisplayName("공개 메시지 전송")
    class SendPublicMessage {

        @Test
        @DisplayName("성공적으로 공개 메시지를 전송한다")
        void success() {
            // given
            String content = "안녕하세요!";
            given(gameRepository.findById(game.getId())).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(sender.getId())).willReturn(Optional.of(sender));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatMessage result = chatService.sendPublicMessage(game.getId(), sender.getId(), content);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getType()).isEqualTo(MessageType.PUBLIC);
            assertThat(result.getSender()).isEqualTo(sender);
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("빈 메시지는 전송할 수 없다")
        void failWithEmptyContent() {
            // given
            String content = "";

            // when & then
            assertThatThrownBy(() -> chatService.sendPublicMessage(game.getId(), sender.getId(), content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비어있습니다");
        }

        @Test
        @DisplayName("500자를 초과하는 메시지는 전송할 수 없다")
        void failWithTooLongContent() {
            // given
            String content = "a".repeat(501);

            // when & then
            assertThatThrownBy(() -> chatService.sendPublicMessage(game.getId(), sender.getId(), content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("너무 깁니다");
        }

        @Test
        @DisplayName("사망한 플레이어는 메시지를 전송할 수 없다")
        void failWhenPlayerDead() {
            // given
            String content = "살려주세요!";
            GamePlayer deadPlayer = GamePlayer.builder()
                    .id(UUID.randomUUID())
                    .game(game)
                    .user(senderUser)
                    .role(GameRole.SUSPECT)
                    .isAlive(false)
                    .build();
            game.getPlayers().add(deadPlayer);

            given(gameRepository.findById(game.getId())).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(deadPlayer.getId())).willReturn(Optional.of(deadPlayer));

            // when & then
            assertThatThrownBy(() -> chatService.sendPublicMessage(game.getId(), deadPlayer.getId(), content))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("사망한 플레이어");
        }

        @Test
        @DisplayName("게임에 참여하지 않은 플레이어는 메시지를 전송할 수 없다")
        void failWhenPlayerNotInGame() {
            // given
            String content = "몰래 참여!";
            GamePlayer outsider = GamePlayer.builder()
                    .id(UUID.randomUUID())
                    .user(User.builder().id(UUID.randomUUID()).nickname("외부인").build())
                    .role(GameRole.SUSPECT)
                    .isAlive(true)
                    .build();

            given(gameRepository.findById(game.getId())).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(outsider.getId())).willReturn(Optional.of(outsider));

            // when & then
            assertThatThrownBy(() -> chatService.sendPublicMessage(game.getId(), outsider.getId(), content))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("참여하지 않은");
        }
    }

    @Nested
    @DisplayName("귓속말 전송")
    class SendWhisper {

        @Test
        @DisplayName("성공적으로 귓속말을 전송한다")
        void success() {
            // given
            String content = "비밀 이야기";
            given(gameRepository.findById(game.getId())).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(sender.getId())).willReturn(Optional.of(sender));
            given(gamePlayerRepository.findById(receiver.getId())).willReturn(Optional.of(receiver));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatMessage result = chatService.sendWhisper(game.getId(), sender.getId(), receiver.getId(), content);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getType()).isEqualTo(MessageType.WHISPER);
            assertThat(result.getSender()).isEqualTo(sender);
            assertThat(result.getReceiver()).isEqualTo(receiver);
        }

        @Test
        @DisplayName("자신에게 귓속말을 보낼 수 없다")
        void failWhenSendToSelf() {
            // given
            String content = "혼잣말";

            // when & then
            assertThatThrownBy(() -> chatService.sendWhisper(game.getId(), sender.getId(), sender.getId(), content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자신에게");
        }
    }

    @Nested
    @DisplayName("시스템 메시지 전송")
    class SendSystemMessage {

        @Test
        @DisplayName("성공적으로 시스템 메시지를 전송한다")
        void success() {
            // given
            String content = "게임이 시작되었습니다!";
            given(gameRepository.findById(game.getId())).willReturn(Optional.of(game));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ChatMessage result = chatService.sendSystemMessage(game.getId(), content);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(content);
            assertThat(result.getType()).isEqualTo(MessageType.SYSTEM);
            assertThat(result.getSender()).isNull();
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class GetMessages {

        @Test
        @DisplayName("공개 메시지를 조회한다")
        void getPublicMessages() {
            // given
            List<ChatMessage> messages = List.of(
                    ChatMessage.createPublic(game, sender, "첫 번째 메시지"),
                    ChatMessage.createPublic(game, receiver, "두 번째 메시지"));
            given(chatMessageRepository.findByGameIdAndTypeOrderByCreatedAtAsc(game.getId(), MessageType.PUBLIC))
                    .willReturn(messages);

            // when
            List<ChatMessage> result = chatService.getPublicMessages(game.getId());

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("최근 메시지를 조회한다")
        void getRecentMessages() {
            // given
            int limit = 10;
            List<ChatMessage> messages = List.of(
                    ChatMessage.createPublic(game, sender, "메시지"));
            given(chatMessageRepository.findRecentMessages(game.getId(), limit))
                    .willReturn(messages);

            // when
            List<ChatMessage> result = chatService.getRecentMessages(game.getId(), limit);

            // then
            assertThat(result).hasSize(1);
            verify(chatMessageRepository).findRecentMessages(game.getId(), limit);
        }

        @Test
        @DisplayName("limit이 0이하면 기본값 50을 사용한다")
        void getRecentMessagesWithDefaultLimit() {
            // given
            given(chatMessageRepository.findRecentMessages(game.getId(), 50))
                    .willReturn(List.of());

            // when
            chatService.getRecentMessages(game.getId(), 0);

            // then
            verify(chatMessageRepository).findRecentMessages(game.getId(), 50);
        }

        @Test
        @DisplayName("귓속말을 조회한다")
        void getWhisperMessages() {
            // given
            List<ChatMessage> whispers = List.of(
                    ChatMessage.createWhisper(game, sender, receiver, "비밀"));
            given(chatMessageRepository.findWhisperMessages(game.getId(), sender.getId()))
                    .willReturn(whispers);

            // when
            List<ChatMessage> result = chatService.getWhisperMessages(game.getId(), sender.getId());

            // then
            assertThat(result).hasSize(1);
        }
    }
}
