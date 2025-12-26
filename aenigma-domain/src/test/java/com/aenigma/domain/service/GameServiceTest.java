package com.aenigma.domain.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 테스트")
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @InjectMocks
    private GameService gameService;

    private Room room;
    private User user1;
    private User user2;
    private User user3;
    private User user4;

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

        user4 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_user4")
                .nickname("유저4")
                .build();
    }

    @Nested
    @DisplayName("createGame 메서드")
    class CreateGameTest {

        @Test
        @DisplayName("게임 생성 성공 - 첫 번째 라운드")
        void createsGameSuccessfully() {
            // given
            given(gameRepository.countByRoomId(room.getId())).willReturn(0L);
            given(gameRepository.save(any(Game.class))).willAnswer(invocation -> {
                Game game = invocation.getArgument(0);
                return Game.builder()
                        .id(UUID.randomUUID())
                        .room(game.getRoom())
                        .roundNumber(game.getRoundNumber())
                        .phase(game.getPhase())
                        .build();
            });

            // when
            Game result = gameService.createGame(room);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRoundNumber()).isEqualTo(1);
            assertThat(result.getPhase()).isEqualTo(GamePhase.INTRO);
            verify(gameRepository).save(any(Game.class));
        }

        @Test
        @DisplayName("두 번째 게임 생성 시 라운드 번호 증가")
        void incrementsRoundNumberForSecondGame() {
            // given
            given(gameRepository.countByRoomId(room.getId())).willReturn(1L);
            given(gameRepository.save(any(Game.class))).willAnswer(invocation -> {
                Game game = invocation.getArgument(0);
                return Game.builder()
                        .id(UUID.randomUUID())
                        .room(game.getRoom())
                        .roundNumber(game.getRoundNumber())
                        .phase(game.getPhase())
                        .build();
            });

            // when
            Game result = gameService.createGame(room);

            // then
            assertThat(result.getRoundNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("assignRoles 메서드")
    class AssignRolesTest {

        @Test
        @DisplayName("4명 기준 역할 배정 - 범인 1, 탐정 1, 용의자 2")
        void assignsRolesCorrectly() {
            // given
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .players(new ArrayList<>())
                    .build();

            List<RoomMember> members = List.of(
                    RoomMember.builder().user(user1).room(room).build(),
                    RoomMember.builder().user(user2).room(room).build(),
                    RoomMember.builder().user(user3).room(room).build(),
                    RoomMember.builder().user(user4).room(room).build());

            given(gamePlayerRepository.save(any(GamePlayer.class))).willAnswer(invocation -> {
                GamePlayer player = invocation.getArgument(0);
                return GamePlayer.builder()
                        .id(UUID.randomUUID())
                        .game(player.getGame())
                        .user(player.getUser())
                        .role(player.getRole())
                        .isAlive(true)
                        .build();
            });

            // when
            List<GamePlayer> players = gameService.assignRoles(game, members);

            // then
            assertThat(players).hasSize(4);

            long criminalCount = players.stream().filter(p -> p.getRole() == GameRole.CRIMINAL).count();
            long detectiveCount = players.stream().filter(p -> p.getRole() == GameRole.DETECTIVE).count();
            long suspectCount = players.stream().filter(p -> p.getRole() == GameRole.SUSPECT).count();

            assertThat(criminalCount).isEqualTo(1);
            assertThat(detectiveCount).isEqualTo(1);
            assertThat(suspectCount).isEqualTo(2);
        }

        @Test
        @DisplayName("2명 기준 역할 배정 - 범인 1, 탐정 1")
        void assignsRolesForTwoPlayers() {
            // given
            Game game = Game.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .roundNumber(1)
                    .players(new ArrayList<>())
                    .build();

            List<RoomMember> members = List.of(
                    RoomMember.builder().user(user1).room(room).build(),
                    RoomMember.builder().user(user2).room(room).build());

            given(gamePlayerRepository.save(any(GamePlayer.class))).willAnswer(invocation -> {
                GamePlayer player = invocation.getArgument(0);
                return GamePlayer.builder()
                        .id(UUID.randomUUID())
                        .game(player.getGame())
                        .user(player.getUser())
                        .role(player.getRole())
                        .isAlive(true)
                        .build();
            });

            // when
            List<GamePlayer> players = gameService.assignRoles(game, members);

            // then
            assertThat(players).hasSize(2);
            assertThat(players.stream().anyMatch(p -> p.getRole() == GameRole.CRIMINAL)).isTrue();
            assertThat(players.stream().anyMatch(p -> p.getRole() == GameRole.DETECTIVE)).isTrue();
        }
    }

    @Nested
    @DisplayName("startGame 메서드")
    class StartGameTest {

        @Test
        @DisplayName("게임 시작 성공")
        void startsGameSuccessfully() {
            // given
            UUID gameId = UUID.randomUUID();
            Game game = Game.builder()
                    .id(gameId)
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.INTRO)
                    .build();

            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(gameRepository.save(any(Game.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Game result = gameService.startGame(gameId);

            // then
            assertThat(result.getPhase()).isEqualTo(GamePhase.LOBBY);
            assertThat(result.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 게임 시작 시 예외 발생")
        void throwsExceptionForNonExistentGame() {
            // given
            UUID gameId = UUID.randomUUID();
            given(gameRepository.findById(gameId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameService.startGame(gameId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("게임을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("nextPhase 메서드")
    class NextPhaseTest {

        @Test
        @DisplayName("LOBBY에서 INVESTIGATION으로 진행")
        void progressesFromLobbyToInvestigation() {
            // given
            UUID gameId = UUID.randomUUID();
            Game game = Game.builder()
                    .id(gameId)
                    .room(room)
                    .roundNumber(1)
                    .phase(GamePhase.LOBBY)
                    .players(new ArrayList<>())
                    .build();

            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(gameRepository.save(any(Game.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Game result = gameService.nextPhase(gameId);

            // then
            assertThat(result.getPhase()).isEqualTo(GamePhase.INVESTIGATION);
        }
    }

    @Nested
    @DisplayName("eliminatePlayer 메서드")
    class EliminatePlayerTest {

        @Test
        @DisplayName("플레이어 제거 성공")
        void eliminatesPlayerSuccessfully() {
            // given
            UUID gameId = UUID.randomUUID();
            UUID userId = user1.getId();

            GamePlayer player = GamePlayer.builder()
                    .id(UUID.randomUUID())
                    .user(user1)
                    .role(GameRole.SUSPECT)
                    .isAlive(true)
                    .build();

            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId)).willReturn(Optional.of(player));
            given(gamePlayerRepository.save(any(GamePlayer.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            GamePlayer result = gameService.eliminatePlayer(gameId, userId);

            // then
            assertThat(result.getIsAlive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 플레이어 제거 시 예외 발생")
        void throwsExceptionForNonExistentPlayer() {
            // given
            UUID gameId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> gameService.eliminatePlayer(gameId, userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("플레이어를 찾을 수 없습니다");
        }
    }
}
