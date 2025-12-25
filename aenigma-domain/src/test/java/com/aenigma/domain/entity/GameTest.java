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

import static org.assertj.core.api.Assertions.*;

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

            game.start();

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

    @Nested
    @DisplayName("승리 조건 체크")
    class WinCondition {

        @Test
        @DisplayName("범인이 모두 검거되면(사망하면) 시민 팀 승리")
        void citizensWinWhenAllCriminalsDead() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INVESTIGATION)
                    .build();

            GamePlayer criminal = GamePlayer.create(game, user1, GameRole.CRIMINAL);
            criminal.eliminate(); // 범인 검거/사망

            GamePlayer suspect1 = GamePlayer.create(game, user2, GameRole.SUSPECT);
            GamePlayer detective = GamePlayer.create(game, user3, GameRole.DETECTIVE);

            game.getPlayers().add(criminal);
            game.getPlayers().add(suspect1);
            game.getPlayers().add(detective);

            // checkWinCondition 내부 로직에 따라 INVESTIGATION/FINAL_VOTE 상태여야 함
            boolean ended = game.checkWinCondition();

            assertThat(ended).isTrue();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINISHED);
            assertThat(game.getWinnerTeam()).isEqualTo(GameRole.SUSPECT);
        }

        @Test
        @DisplayName("범인 수가 시민 수 이상이면 범인 팀 승리")
        void criminalsWinWhenEqualOrMore() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INVESTIGATION)
                    .build();

            GamePlayer criminal = GamePlayer.create(game, user1, GameRole.CRIMINAL);
            GamePlayer suspect = GamePlayer.create(game, user2, GameRole.SUSPECT);

            game.getPlayers().add(criminal);
            game.getPlayers().add(suspect);

            boolean ended = game.checkWinCondition();

            assertThat(ended).isTrue();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINISHED);
            assertThat(game.getWinnerTeam()).isEqualTo(GameRole.CRIMINAL);
        }
    }
}
