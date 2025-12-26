package com.aenigma.domain.entity;

import com.aenigma.domain.chat.entity.ChatMessage;
import com.aenigma.domain.chat.entity.MessageType;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessage 엔티티 테스트")
class ChatMessageTest {

    private Room room;
    private Game game;
    private User user1;
    private User user2;
    private GamePlayer sender;
    private GamePlayer receiver;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("ABC123")
                .title("테스트 방")
                .build();

        game = Game.builder()
                .id(UUID.randomUUID())
                .room(room)
                .roundNumber(1)
                .build();

        user1 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_sender")
                .nickname("발신자")
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_receiver")
                .nickname("수신자")
                .build();

        sender = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user1)
                .role(GameRole.SUSPECT)
                .build();

        receiver = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user2)
                .role(GameRole.DETECTIVE)
                .build();
    }

    @Nested
    @DisplayName("createPublic 팩토리 메서드")
    class CreatePublicTest {

        @Test
        @DisplayName("PUBLIC 메시지 생성 성공")
        void createsPublicMessageSuccessfully() {
            // given
            String content = "안녕하세요, 모두!";

            // when
            ChatMessage message = ChatMessage.createPublic(game, sender, content);

            // then
            assertThat(message.getGame()).isEqualTo(game);
            assertThat(message.getSender()).isEqualTo(sender);
            assertThat(message.getContent()).isEqualTo(content);
            assertThat(message.getType()).isEqualTo(MessageType.PUBLIC);
            assertThat(message.getReceiver()).isNull();
        }
    }

    @Nested
    @DisplayName("createWhisper 팩토리 메서드")
    class CreateWhisperTest {

        @Test
        @DisplayName("WHISPER 메시지 생성 성공")
        void createsWhisperMessageSuccessfully() {
            // given
            String content = "귓속말 테스트입니다.";

            // when
            ChatMessage message = ChatMessage.createWhisper(game, sender, receiver, content);

            // then
            assertThat(message.getGame()).isEqualTo(game);
            assertThat(message.getSender()).isEqualTo(sender);
            assertThat(message.getReceiver()).isEqualTo(receiver);
            assertThat(message.getContent()).isEqualTo(content);
            assertThat(message.getType()).isEqualTo(MessageType.WHISPER);
        }

        @Test
        @DisplayName("WHISPER 메시지는 receiver가 필수")
        void whisperMessageHasReceiver() {
            // given
            String content = "귓속말";

            // when
            ChatMessage message = ChatMessage.createWhisper(game, sender, receiver, content);

            // then
            assertThat(message.getReceiver()).isNotNull();
            assertThat(message.getReceiver().getUser().getNickname()).isEqualTo("수신자");
        }
    }

    @Nested
    @DisplayName("createSystem 팩토리 메서드")
    class CreateSystemTest {

        @Test
        @DisplayName("SYSTEM 메시지 생성 성공")
        void createsSystemMessageSuccessfully() {
            // given
            String content = "게임이 시작되었습니다.";

            // when
            ChatMessage message = ChatMessage.createSystem(game, content);

            // then
            assertThat(message.getGame()).isEqualTo(game);
            assertThat(message.getSender()).isNull();
            assertThat(message.getReceiver()).isNull();
            assertThat(message.getContent()).isEqualTo(content);
            assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM 메시지는 sender가 null")
        void systemMessageHasNoSender() {
            // given
            String content = "시스템 알림";

            // when
            ChatMessage message = ChatMessage.createSystem(game, content);

            // then
            assertThat(message.getSender()).isNull();
        }
    }
}
