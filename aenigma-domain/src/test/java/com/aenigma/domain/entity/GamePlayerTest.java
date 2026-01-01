package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GamePlayer 엔티티 테스트
 */
@DisplayName("GamePlayer 엔티티 테스트")
class GamePlayerTest {

    private Game game;
    private User user;

    @BeforeEach
    void setUp() {
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

        user = User.builder()
                .username("GUEST_player")
                .nickname("Player")
                .build();
    }

    @Nested
    @DisplayName("create 팩토리 메서드 테스트")
    class CreateTest {

        @Test
        @DisplayName("GamePlayer를 올바르게 생성한다")
        void createsGamePlayerCorrectly() {
            // when
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            // then
            assertThat(player.getGame()).isEqualTo(game);
            assertThat(player.getUser()).isEqualTo(user);
            assertThat(player.getRole()).isEqualTo(GameRole.SUSPECT);
            assertThat(player.getIsAlive()).isTrue();
        }

        @Test
        @DisplayName("CRIMINAL 역할로 생성")
        void createsWithCriminalRole() {
            // when
            GamePlayer player = GamePlayer.create(game, user, GameRole.CRIMINAL);

            // then
            assertThat(player.getRole()).isEqualTo(GameRole.CRIMINAL);
        }

        @Test
        @DisplayName("DETECTIVE 역할로 생성")
        void createsWithDetectiveRole() {
            // when
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            // then
            assertThat(player.getRole()).isEqualTo(GameRole.DETECTIVE);
        }
    }

    @Nested
    @DisplayName("eliminate 테스트")
    class EliminateTest {

        @Test
        @DisplayName("플레이어 제거 시 isAlive가 false가 된다")
        void eliminateSetsIsAliveFalse() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);
            assertThat(player.getIsAlive()).isTrue();

            // when
            player.eliminate();

            // then
            assertThat(player.getIsAlive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isCriminalTeam 테스트")
    class IsCriminalTeamTest {

        @Test
        @DisplayName("CRIMINAL 역할은 true 반환")
        void criminalReturnsTrue() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.CRIMINAL);

            // when & then
            assertThat(player.isCriminalTeam()).isTrue();
        }

        @Test
        @DisplayName("SUSPECT 역할은 false 반환")
        void suspectReturnsFalse() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            // when & then
            assertThat(player.isCriminalTeam()).isFalse();
        }

        @Test
        @DisplayName("DETECTIVE 역할은 false 반환")
        void detectiveReturnsFalse() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            // when & then
            assertThat(player.isCriminalTeam()).isFalse();
        }
    }

    @Nested
    @DisplayName("isCitizenTeam 테스트")
    class IsCitizenTeamTest {

        @Test
        @DisplayName("CRIMINAL 역할은 false 반환")
        void criminalReturnsFalse() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.CRIMINAL);

            // when & then
            assertThat(player.isCitizenTeam()).isFalse();
        }

        @Test
        @DisplayName("SUSPECT 역할은 true 반환")
        void suspectReturnsTrue() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            // when & then
            assertThat(player.isCitizenTeam()).isTrue();
        }

        @Test
        @DisplayName("DETECTIVE 역할은 true 반환")
        void detectiveReturnsTrue() {
            // given
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            // when & then
            assertThat(player.isCitizenTeam()).isTrue();
        }
    }
}
