package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Game 엔티티 테스트")
class GameTest {

    private Room room;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("ABC123")
                .title("테스트 방")
                .build();

        user1 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_user1")
                .nickname("유저1")
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_user2")
                .nickname("유저2")
                .build();

        user3 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_user3")
                .nickname("유저3")
                .build();
    }

    @Nested
    @DisplayName("게임 시작")
    class StartGame {

        @Test
        @DisplayName("준비 상태에서 게임을 시작할 수 있다")
        void canStartFromPreparing() {
            // given
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INTRO)
                    .build();

            // when
            game.start();

            // then
            assertThat(game.getPhase()).isEqualTo(GamePhase.INVESTIGATION);
            assertThat(game.getDayCount()).isEqualTo(1);
            assertThat(game.getStartedAt()).isNotNull();
        }

        @Test
        void cannotStartFromOtherPhase() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INVESTIGATION)
                    .build();

            assertThatThrownBy(game::start)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("게임 진행")
    class GameProgress {

        @Test
        @DisplayName("INTRO -> INVESTIGATION -> FINAL_VOTE -> CONCLUSION -> FINISHED 순서로 진행된다")
        void phaseProgressesCorrectly() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INTRO)
                    .dayCount(0)
                    .build();

            // INTRO -> INVESTIGATION
            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.INVESTIGATION);

            // INVESTIGATION -> FINAL_VOTE
            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINAL_VOTE);

            // FINAL_VOTE -> CONCLUSION
            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.CONCLUSION);

            // CONCLUSION -> FINISHED
            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINISHED);
        }
    }
}
