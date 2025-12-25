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

@DisplayName("GamePlayer 엔티티 테스트")
class GamePlayerTest {

    private Game game;
    private User user;

    @BeforeEach
    void setUp() {
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("ABC123")
                .title("테스트 방")
                .build();

        game = Game.builder()
                .id(UUID.randomUUID())
                .room(room)
                .roundNumber(1)
                .phase(GamePhase.INVESTIGATION)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_test")
                .nickname("테스터")
                .build();
    }

    @Nested
    @DisplayName("플레이어 생성")
    class CreatePlayer {

        @Test
        @DisplayName("기본 상태로 생성된다")
        void createdWithDefaultState() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            assertThat(player.getGame()).isEqualTo(game);
            assertThat(player.getUser()).isEqualTo(user);
            assertThat(player.getRole()).isEqualTo(GameRole.SUSPECT);
            assertThat(player.getIsAlive()).isTrue();
        }
    }

    @Nested
    @DisplayName("플레이어 제거")
    class EliminatePlayer {

        @Test
        @DisplayName("플레이어를 제거하면 사망 상태가 된다")
        void playerIsEliminated() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            player.eliminate();

            assertThat(player.getIsAlive()).isFalse();
        }
    }

    @Nested
    @DisplayName("팀 확인")
    class TeamCheck {

        @Test
        @DisplayName("범인은 범인 팀이다")
        void criminalIsCriminalTeam() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CRIMINAL);

            assertThat(player.isCriminalTeam()).isTrue();
            assertThat(player.isCitizenTeam()).isFalse();
        }

        @Test
        @DisplayName("탐정은 시민 팀이다")
        void detectiveIsCitizenTeam() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            assertThat(player.isCriminalTeam()).isFalse();
            assertThat(player.isCitizenTeam()).isTrue();
        }

        @Test
        @DisplayName("용의자는 시민 팀이다")
        void suspectIsCitizenTeam() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.SUSPECT);

            assertThat(player.isCriminalTeam()).isFalse();
            assertThat(player.isCitizenTeam()).isTrue();
        }
    }
}
