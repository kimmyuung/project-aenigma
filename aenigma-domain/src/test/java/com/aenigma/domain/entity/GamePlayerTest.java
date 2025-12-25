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
                .phase(GamePhase.DAY)
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
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);

            assertThat(player.getGame()).isEqualTo(game);
            assertThat(player.getUser()).isEqualTo(user);
            assertThat(player.getRole()).isEqualTo(GameRole.CITIZEN);
            assertThat(player.getIsAlive()).isTrue();
            assertThat(player.getHasVoted()).isFalse();
            assertThat(player.getHasUsedSkill()).isFalse();
            assertThat(player.getIsProtected()).isFalse();
        }
    }

    @Nested
    @DisplayName("플레이어 제거")
    class EliminatePlayer {

        @Test
        @DisplayName("보호되지 않은 플레이어는 제거된다")
        void unprotectedPlayerIsEliminated() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);

            player.eliminate();

            assertThat(player.getIsAlive()).isFalse();
        }

        @Test
        @DisplayName("보호된 플레이어는 제거되지 않는다")
        void protectedPlayerSurvives() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);
            player.protect();

            player.eliminate();

            assertThat(player.getIsAlive()).isTrue();
        }

        @Test
        @DisplayName("처형은 보호를 무시한다")
        void executeIgnoresProtection() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);
            player.protect();

            player.execute();

            assertThat(player.getIsAlive()).isFalse();
        }
    }

    @Nested
    @DisplayName("투표")
    class Voting {

        @Test
        @DisplayName("생존한 플레이어는 투표할 수 있다")
        void alivePlayerCanVote() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);

            player.vote();

            assertThat(player.getHasVoted()).isTrue();
        }

        @Test
        @DisplayName("사망한 플레이어는 투표할 수 없다")
        void deadPlayerCannotVote() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);
            player.execute();

            assertThatThrownBy(player::vote)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("사망한 플레이어");
        }
    }

    @Nested
    @DisplayName("스킬 사용")
    class SkillUsage {

        @Test
        @DisplayName("탐정은 스킬을 사용할 수 있다")
        void detectiveCanUseSkill() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            player.useSkill();

            assertThat(player.getHasUsedSkill()).isTrue();
        }

        @Test
        @DisplayName("시민은 스킬을 사용할 수 없다")
        void citizenCannotUseSkill() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.CITIZEN);

            assertThatThrownBy(player::useSkill)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("시민은 스킬이 없습니다");
        }
    }

    @Nested
    @DisplayName("턴 초기화")
    class ResetTurn {

        @Test
        @DisplayName("투표, 스킬 사용, 보호 상태가 초기화된다")
        void resetsAllTurnState() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);
            player.vote();
            player.useSkill();
            player.protect();

            player.resetTurn();

            assertThat(player.getHasVoted()).isFalse();
            assertThat(player.getHasUsedSkill()).isFalse();
            assertThat(player.getIsProtected()).isFalse();
        }
    }

    @Nested
    @DisplayName("팀 확인")
    class TeamCheck {

        @Test
        @DisplayName("범인은 범인 팀이다")
        void killerIsKillerTeam() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.KILLER);

            assertThat(player.isKillerTeam()).isTrue();
            assertThat(player.isCitizenTeam()).isFalse();
        }

        @Test
        @DisplayName("탐정은 시민 팀이다")
        void detectiveIsCitizenTeam() {
            GamePlayer player = GamePlayer.create(game, user, GameRole.DETECTIVE);

            assertThat(player.isKillerTeam()).isFalse();
            assertThat(player.isCitizenTeam()).isTrue();
        }
    }
}
