package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GameClue;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.scenario.entity.ClueType;
import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.entity.ScenarioClue;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GameClue 엔티티 테스트
 */
@DisplayName("GameClue 엔티티 테스트")
class GameClueTest {

        private Game game;
        private GamePlayer player1;
        private GamePlayer player2;
        private Scenario scenario;
        private ScenarioClue scenarioClue;

        @BeforeEach
        void setUp() {
                User user1 = User.builder()
                                .nickname("Player1")
                                .username("GUEST_player1")
                                .build();

                User user2 = User.builder()
                                .nickname("Player2")
                                .username("GUEST_player2")
                                .build();

                Room room = Room.builder()
                                .title("Test Room")
                                .roomCode("ABC123")
                                .host(user1)
                                .maxPlayers(6)
                                .build();

                scenario = Scenario.builder()
                                .title("Test Scenario")
                                .author(user1)
                                .minPlayers(4)
                                .maxPlayers(6)
                                .price(BigDecimal.ZERO)
                                .build();

                game = Game.builder()
                                .room(room)
                                .scenario(scenario)
                                .roundNumber(1)
                                .build();

                player1 = GamePlayer.builder()
                                .game(game)
                                .user(user1)
                                .role(GameRole.SUSPECT)
                                .isAlive(true)
                                .build();

                player2 = GamePlayer.builder()
                                .game(game)
                                .user(user2)
                                .role(GameRole.SUSPECT)
                                .isAlive(true)
                                .build();

                scenarioClue = ScenarioClue.builder()
                                .scenario(scenario)
                                .title("중요한 단서")
                                .content("피해자의 손에서 발견된 편지 조각")
                                .clueType(ClueType.PUBLIC)
                                .importance(5)
                                .revealRound(1)
                                .build();
        }

        @Nested
        @DisplayName("discover 테스트")
        class DiscoverTest {

                @Test
                @DisplayName("단서 발견 시 상태가 변경된다")
                void discoverSetsStateCorrectly() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title(scenarioClue.getTitle())
                                        .content(scenarioClue.getContent())
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(false)
                                        .build();

                        // when
                        gameClue.discover(player1);

                        // then
                        assertThat(gameClue.getIsDiscovered()).isTrue();
                        assertThat(gameClue.getDiscoveredBy()).isEqualTo(player1);
                }
        }

        @Nested
        @DisplayName("isDiscoverable 테스트")
        class IsDiscoverableTest {

                @Test
                @DisplayName("이미 발견된 단서는 발견 불가")
                void alreadyDiscoveredClueIsNotDiscoverable() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title(scenarioClue.getTitle())
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(true)
                                        .revealRound(1)
                                        .build();

                        // when & then
                        assertThat(gameClue.isDiscoverable(1)).isFalse();
                        assertThat(gameClue.isDiscoverable(5)).isFalse();
                }

                @Test
                @DisplayName("revealRound 이전에는 발견 불가")
                void cannotDiscoverBeforeRevealRound() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title(scenarioClue.getTitle())
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(false)
                                        .revealRound(3)
                                        .build();

                        // when & then
                        assertThat(gameClue.isDiscoverable(1)).isFalse();
                        assertThat(gameClue.isDiscoverable(2)).isFalse();
                }

                @Test
                @DisplayName("revealRound 이후에는 발견 가능")
                void canDiscoverAfterRevealRound() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title(scenarioClue.getTitle())
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(false)
                                        .revealRound(2)
                                        .build();

                        // when & then
                        assertThat(gameClue.isDiscoverable(2)).isTrue();
                        assertThat(gameClue.isDiscoverable(3)).isTrue();
                }

                @Test
                @DisplayName("revealRound가 null이면 항상 발견 가능")
                void noRevealRoundMeansAlwaysDiscoverable() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title(scenarioClue.getTitle())
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(false)
                                        .revealRound(null)
                                        .build();

                        // when & then
                        assertThat(gameClue.isDiscoverable(1)).isTrue();
                        assertThat(gameClue.isDiscoverable(100)).isTrue();
                }
        }

        @Nested
        @DisplayName("isVisibleTo 테스트")
        class IsVisibleToTest {

                @Test
                @DisplayName("발견된 PUBLIC 단서는 모두에게 보인다")
                void discoveredPublicClueIsVisibleToAll() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title("공개 단서")
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(true)
                                        .discoveredBy(player1)
                                        .build();

                        // when & then
                        assertThat(gameClue.isVisibleTo(player1)).isTrue();
                        assertThat(gameClue.isVisibleTo(player2)).isTrue();
                }

                @Test
                @DisplayName("발견되지 않은 PUBLIC 단서는 보이지 않는다")
                void undiscoveredPublicClueIsNotVisible() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title("공개 단서")
                                        .clueType(ClueType.PUBLIC)
                                        .isDiscovered(false)
                                        .build();

                        // when & then
                        assertThat(gameClue.isVisibleTo(player1)).isFalse();
                        assertThat(gameClue.isVisibleTo(player2)).isFalse();
                }

                @Test
                @DisplayName("PERSONAL 단서는 배정된 플레이어만 볼 수 있다")
                void personalClueOnlyVisibleToAssignedPlayer() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title("개인 단서")
                                        .clueType(ClueType.PERSONAL)
                                        .assignedPlayer(player1)
                                        .isDiscovered(false)
                                        .build();

                        // when & then
                        assertThat(gameClue.isVisibleTo(player1)).isTrue();
                        assertThat(gameClue.isVisibleTo(player2)).isFalse();
                }

                @Test
                @DisplayName("HIDDEN 단서는 발견자만 볼 수 있다")
                void hiddenClueOnlyVisibleToDiscoverer() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title("숨겨진 단서")
                                        .clueType(ClueType.HIDDEN)
                                        .isDiscovered(true)
                                        .discoveredBy(player1)
                                        .build();

                        // when & then
                        assertThat(gameClue.isVisibleTo(player1)).isTrue();
                        assertThat(gameClue.isVisibleTo(player2)).isFalse();
                }

                @Test
                @DisplayName("발견되지 않은 HIDDEN 단서는 누구도 볼 수 없다")
                void undiscoveredHiddenClueIsInvisible() {
                        // given
                        GameClue gameClue = GameClue.builder()
                                        .game(game)
                                        .scenarioClue(scenarioClue)
                                        .title("숨겨진 단서")
                                        .clueType(ClueType.HIDDEN)
                                        .isDiscovered(false)
                                        .build();

                        // when & then
                        assertThat(gameClue.isVisibleTo(player1)).isFalse();
                        assertThat(gameClue.isVisibleTo(player2)).isFalse();
                }
        }

        @Nested
        @DisplayName("from 팩토리 메서드 테스트")
        class FromFactoryMethodTest {

                @Test
                @DisplayName("ScenarioClue에서 GameClue를 올바르게 생성한다")
                void createsGameClueFromScenarioClue() {
                        // given
                        ScenarioClue publicClue = ScenarioClue.builder()
                                        .scenario(scenario)
                                        .title("공개 단서")
                                        .content("단서 내용")
                                        .clueType(ClueType.PUBLIC)
                                        .importance(4)
                                        .revealRound(2)
                                        .imageUrl("http://example.com/image.png")
                                        .build();

                        // when
                        GameClue gameClue = GameClue.from(publicClue, game, null);

                        // then
                        assertThat(gameClue.getGame()).isEqualTo(game);
                        assertThat(gameClue.getScenarioClue()).isEqualTo(publicClue);
                        assertThat(gameClue.getTitle()).isEqualTo("공개 단서");
                        assertThat(gameClue.getContent()).isEqualTo("단서 내용");
                        assertThat(gameClue.getClueType()).isEqualTo(ClueType.PUBLIC);
                        assertThat(gameClue.getImportance()).isEqualTo(4);
                        assertThat(gameClue.getRevealRound()).isEqualTo(2);
                        assertThat(gameClue.getImageUrl()).isEqualTo("http://example.com/image.png");
                        // PUBLIC 단서는 생성 시 자동으로 discovered
                        assertThat(gameClue.getIsDiscovered()).isTrue();
                }

                @Test
                @DisplayName("PERSONAL 단서는 배정된 플레이어와 함께 생성된다")
                void createsPersonalClueWithAssignedPlayer() {
                        // given
                        ScenarioClue personalClue = ScenarioClue.builder()
                                        .scenario(scenario)
                                        .title("개인 단서")
                                        .content("비밀 내용")
                                        .clueType(ClueType.PERSONAL)
                                        .importance(5)
                                        .build();

                        // when
                        GameClue gameClue = GameClue.from(personalClue, game, player1);

                        // then
                        assertThat(gameClue.getAssignedPlayer()).isEqualTo(player1);
                        assertThat(gameClue.getClueType()).isEqualTo(ClueType.PERSONAL);
                        // PERSONAL은 자동 discovered 아님
                        assertThat(gameClue.getIsDiscovered()).isFalse();
                }
        }
}
