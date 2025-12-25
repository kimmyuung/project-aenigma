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
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.PREPARING)
                    .build();

            game.start();

            assertThat(game.getPhase()).isEqualTo(GamePhase.DAY);
            assertThat(game.getDayCount()).isEqualTo(1);
            assertThat(game.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("준비 상태가 아니면 게임을 시작할 수 없다")
        void cannotStartFromOtherPhase() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.DAY)
                    .build();

            assertThatThrownBy(game::start)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("준비 단계에서만");
        }
    }

    @Nested
    @DisplayName("게임 진행")
    class GameProgress {

        @Test
        @DisplayName("낮 -> 투표 -> 밤 -> 낮 순서로 진행된다")
        void phaseProgressesCorrectly() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.DAY)
                    .dayCount(1)
                    .build();

            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.VOTING);

            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.NIGHT);

            game.nextPhase();
            assertThat(game.getPhase()).isEqualTo(GamePhase.DAY);
            assertThat(game.getDayCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("승리 조건 체크")
    class WinCondition {

        @Test
        @DisplayName("범인이 모두 사망하면 시민 팀 승리")
        void citizensWinWhenAllKillersDead() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.DAY)
                    .build();

            GamePlayer killer = GamePlayer.create(game, user1, GameRole.KILLER);
            killer.execute(); // 범인 사망

            GamePlayer citizen1 = GamePlayer.create(game, user2, GameRole.CITIZEN);
            GamePlayer citizen2 = GamePlayer.create(game, user3, GameRole.DETECTIVE);

            game.getPlayers().add(killer);
            game.getPlayers().add(citizen1);
            game.getPlayers().add(citizen2);

            boolean ended = game.checkWinCondition();

            assertThat(ended).isTrue();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINISHED);
            assertThat(game.getWinnerTeam()).isEqualTo(GameRole.CITIZEN);
        }

        @Test
        @DisplayName("범인 수가 시민 수 이상이면 범인 팀 승리")
        void killersWinWhenEqualOrMore() {
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.DAY)
                    .build();

            GamePlayer killer = GamePlayer.create(game, user1, GameRole.KILLER);
            GamePlayer citizen = GamePlayer.create(game, user2, GameRole.CITIZEN);

            game.getPlayers().add(killer);
            game.getPlayers().add(citizen);

            boolean ended = game.checkWinCondition();

            assertThat(ended).isTrue();
            assertThat(game.getPhase()).isEqualTo(GamePhase.FINISHED);
            assertThat(game.getWinnerTeam()).isEqualTo(GameRole.KILLER);
        }
    }
}
