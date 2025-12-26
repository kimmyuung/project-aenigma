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
                @DisplayName("INTRO 상태에서 게임을 시작할 수 있다")
                void canStartFromIntro() {
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
                        assertThat(game.getPhase()).isEqualTo(GamePhase.LOBBY);
                        assertThat(game.getDayCount()).isEqualTo(1);
                        assertThat(game.getInvestigationRound()).isEqualTo(1);
                        assertThat(game.getStartedAt()).isNotNull();
                }

                @Test
                @DisplayName("INTRO가 아닌 페이즈에서는 시작 불가")
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
                @DisplayName("INTRO -> LOBBY -> INVESTIGATION -> FINAL_VOTE -> CONCLUSION -> FINISHED 순서로 진행된다")
                void phaseProgressesCorrectly() {
                        Game game = Game.builder()
                                        .id(UUID.randomUUID())
                                        .room(room)
                                        .roundNumber(1)
                                        .phase(GamePhase.INTRO)
                                        .dayCount(0)
                                        .maxInvestigationRounds(1) // 1라운드로 설정
                                        .build();

                        // INTRO -> LOBBY
                        game.nextPhase();
                        assertThat(game.getPhase()).isEqualTo(GamePhase.LOBBY);

                        // LOBBY -> INVESTIGATION
                        game.nextPhase();
                        assertThat(game.getPhase()).isEqualTo(GamePhase.INVESTIGATION);

                        // INVESTIGATION -> FINAL_VOTE (maxInvestigationRounds = 1이므로 바로 투표로)
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
        @DisplayName("조사 라운드")
        class InvestigationRoundTest {

                @Test
                @DisplayName("조사 라운드가 최대 라운드보다 작으면 라운드만 증가")
                void incrementsRoundWhenBelowMax() {
                        Game game = Game.builder()
                                        .id(UUID.randomUUID())
                                        .room(room)
                                        .roundNumber(1)
                                        .phase(GamePhase.INVESTIGATION)
                                        .investigationRound(1)
                                        .maxInvestigationRounds(3)
                                        .build();

                        // 1 -> 2
                        game.nextPhase();
                        assertThat(game.getPhase()).isEqualTo(GamePhase.INVESTIGATION);
                        assertThat(game.getInvestigationRound()).isEqualTo(2);

                        // 2 -> 3
                        game.nextPhase();
                        assertThat(game.getPhase()).isEqualTo(GamePhase.INVESTIGATION);
                        assertThat(game.getInvestigationRound()).isEqualTo(3);

                        // 3 -> FINAL_VOTE
                        game.nextPhase();
                        assertThat(game.getPhase()).isEqualTo(GamePhase.FINAL_VOTE);
                }

                @Test
                @DisplayName("최대 조사 라운드 설정 성공")
                void setsMaxInvestigationRoundsSuccessfully() {
                        Game game = Game.builder()
                                        .id(UUID.randomUUID())
                                        .room(room)
                                        .roundNumber(1)
                                        .build();

                        game.setMaxInvestigationRounds(3);

                        assertThat(game.getMaxInvestigationRounds()).isEqualTo(3);
                }

                @Test
                @DisplayName("최대 조사 라운드는 1~3 범위만 허용")
                void throwsExceptionForInvalidMaxRounds() {
                        Game game = Game.builder()
                                        .id(UUID.randomUUID())
                                        .room(room)
                                        .roundNumber(1)
                                        .build();

                        assertThatThrownBy(() -> game.setMaxInvestigationRounds(0))
                                        .isInstanceOf(IllegalArgumentException.class);

                        assertThatThrownBy(() -> game.setMaxInvestigationRounds(4))
                                        .isInstanceOf(IllegalArgumentException.class);
                }

                @Test
                @DisplayName("조사 라운드 정보 문자열 반환")
                void returnsInvestigationRoundInfo() {
                        Game game = Game.builder()
                                        .id(UUID.randomUUID())
                                        .room(room)
                                        .roundNumber(1)
                                        .investigationRound(2)
                                        .maxInvestigationRounds(3)
                                        .build();

                        assertThat(game.getInvestigationRoundInfo()).isEqualTo("2/3");
                }
        }
}
