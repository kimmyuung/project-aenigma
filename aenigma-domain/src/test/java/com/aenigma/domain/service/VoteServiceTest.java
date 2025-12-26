package com.aenigma.domain.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.entity.VoteType;
import com.aenigma.domain.vote.repository.VoteRepository;
import com.aenigma.domain.vote.service.VoteService;
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
@DisplayName("VoteService 테스트")
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePlayerRepository gamePlayerRepository;

    @InjectMocks
    private VoteService voteService;

    private Room room;
    private Game game;
    private User user1;
    private User user2;
    private GamePlayer voter;
    private GamePlayer target;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("ABC123")
                .title("테스트 방")
                .build();

        user1 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_voter")
                .nickname("투표자")
                .build();

        user2 = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_target")
                .nickname("대상자")
                .build();

        game = Game.builder()
                .id(UUID.randomUUID())
                .room(room)
                .roundNumber(1)
                .phase(GamePhase.FINAL_VOTE)
                .players(new ArrayList<>())
                .build();

        voter = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user1)
                .role(GameRole.SUSPECT)
                .isAlive(true)
                .build();

        target = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user2)
                .role(GameRole.CRIMINAL)
                .isAlive(true)
                .build();

        game.getPlayers().add(voter);
        game.getPlayers().add(target);
    }

    @Nested
    @DisplayName("castVote 메서드")
    class CastVoteTest {

        @Test
        @DisplayName("투표 성공")
        void castsVoteSuccessfully() {
            // given
            UUID gameId = game.getId();
            UUID voterId = voter.getId();
            UUID targetId = target.getId();
            int round = 1;

            given(voteRepository.existsByGameIdAndVoterIdAndRound(gameId, voterId, round)).willReturn(false);
            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(voterId)).willReturn(Optional.of(voter));
            given(gamePlayerRepository.findById(targetId)).willReturn(Optional.of(target));
            given(voteRepository.save(any(Vote.class))).willAnswer(invocation -> {
                Vote vote = invocation.getArgument(0);
                return Vote.builder()
                        .id(UUID.randomUUID())
                        .game(vote.getGame())
                        .voter(vote.getVoter())
                        .target(vote.getTarget())
                        .voteType(vote.getVoteType())
                        .round(vote.getRound())
                        .build();
            });

            // when
            Vote result = voteService.castVote(gameId, voterId, targetId, VoteType.FINAL_VOTE, round);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getVoteType()).isEqualTo(VoteType.FINAL_VOTE);
            verify(voteRepository).save(any(Vote.class));
        }

        @Test
        @DisplayName("이미 투표한 경우 예외 발생")
        void throwsExceptionWhenAlreadyVoted() {
            // given
            UUID gameId = game.getId();
            UUID voterId = voter.getId();
            UUID targetId = target.getId();
            int round = 1;

            given(voteRepository.existsByGameIdAndVoterIdAndRound(gameId, voterId, round)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> voteService.castVote(gameId, voterId, targetId, VoteType.FINAL_VOTE, round))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 이번 라운드에 투표하셨습니다");
        }

        @Test
        @DisplayName("사망한 플레이어가 투표하려고 하면 예외 발생")
        void throwsExceptionWhenDeadVoterTriesToVote() {
            // given
            UUID gameId = game.getId();
            UUID voterId = voter.getId();
            UUID targetId = target.getId();
            int round = 1;

            GamePlayer deadVoter = GamePlayer.builder()
                    .id(voterId)
                    .game(game)
                    .user(user1)
                    .role(GameRole.SUSPECT)
                    .isAlive(false)
                    .build();

            given(voteRepository.existsByGameIdAndVoterIdAndRound(gameId, voterId, round)).willReturn(false);
            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(voterId)).willReturn(Optional.of(deadVoter));
            given(gamePlayerRepository.findById(targetId)).willReturn(Optional.of(target));

            // when & then
            assertThatThrownBy(() -> voteService.castVote(gameId, voterId, targetId, VoteType.FINAL_VOTE, round))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("사망한 플레이어는 투표할 수 없습니다");
        }

        @Test
        @DisplayName("사망한 플레이어에게 투표하면 예외 발생")
        void throwsExceptionWhenVotingForDeadPlayer() {
            // given
            UUID gameId = game.getId();
            UUID voterId = voter.getId();
            UUID targetId = target.getId();
            int round = 1;

            GamePlayer deadTarget = GamePlayer.builder()
                    .id(targetId)
                    .game(game)
                    .user(user2)
                    .role(GameRole.CRIMINAL)
                    .isAlive(false)
                    .build();

            given(voteRepository.existsByGameIdAndVoterIdAndRound(gameId, voterId, round)).willReturn(false);
            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(gamePlayerRepository.findById(voterId)).willReturn(Optional.of(voter));
            given(gamePlayerRepository.findById(targetId)).willReturn(Optional.of(deadTarget));

            // when & then
            assertThatThrownBy(() -> voteService.castVote(gameId, voterId, targetId, VoteType.FINAL_VOTE, round))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사망한 플레이어에게 투표할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getMostVotedPlayers 메서드")
    class GetMostVotedPlayersTest {

        @Test
        @DisplayName("최다 득표자 조회 성공")
        void getsMostVotedPlayersSuccessfully() {
            // given
            UUID gameId = game.getId();
            int round = 1;

            List<Object[]> voteResults = new ArrayList<>();
            voteResults.add(new Object[] { target.getId(), 3L });
            voteResults.add(new Object[] { voter.getId(), 1L });

            given(voteRepository.getVoteResultsByRound(gameId, round)).willReturn(voteResults);
            given(gamePlayerRepository.findById(target.getId())).willReturn(Optional.of(target));

            // when
            List<GamePlayer> result = voteService.getMostVotedPlayers(gameId, round);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(target.getId());
        }

        @Test
        @DisplayName("투표 결과가 없을 경우 빈 리스트 반환")
        void returnsEmptyListWhenNoVotes() {
            // given
            UUID gameId = game.getId();
            int round = 1;

            given(voteRepository.getVoteResultsByRound(gameId, round)).willReturn(new ArrayList<>());

            // when
            List<GamePlayer> result = voteService.getMostVotedPlayers(gameId, round);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("동점일 경우 여러 명 반환")
        void returnsMultiplePlayersOnTie() {
            // given
            UUID gameId = game.getId();
            int round = 1;

            List<Object[]> voteResults = new ArrayList<>();
            voteResults.add(new Object[] { target.getId(), 2L });
            voteResults.add(new Object[] { voter.getId(), 2L });

            given(voteRepository.getVoteResultsByRound(gameId, round)).willReturn(voteResults);
            given(gamePlayerRepository.findById(target.getId())).willReturn(Optional.of(target));
            given(gamePlayerRepository.findById(voter.getId())).willReturn(Optional.of(voter));

            // when
            List<GamePlayer> result = voteService.getMostVotedPlayers(gameId, round);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("isVotingComplete 메서드")
    class IsVotingCompleteTest {

        @Test
        @DisplayName("모든 생존자가 투표했을 경우 true 반환")
        void returnsTrueWhenAllAlivePlayersVoted() {
            // given
            UUID gameId = game.getId();
            int round = 1;

            List<Vote> votes = List.of(
                    Vote.builder().id(UUID.randomUUID()).voter(voter).target(target).round(round).build(),
                    Vote.builder().id(UUID.randomUUID()).voter(target).target(voter).round(round).build());

            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(voteRepository.findByGameIdAndRound(gameId, round)).willReturn(votes);

            // when
            boolean result = voteService.isVotingComplete(gameId, round);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("일부 생존자만 투표했을 경우 false 반환")
        void returnsFalseWhenNotAllPlayersVoted() {
            // given
            UUID gameId = game.getId();
            int round = 1;

            List<Vote> votes = List.of(
                    Vote.builder().id(UUID.randomUUID()).voter(voter).target(target).round(round).build());

            given(gameRepository.findById(gameId)).willReturn(Optional.of(game));
            given(voteRepository.findByGameIdAndRound(gameId, round)).willReturn(votes);

            // when
            boolean result = voteService.isVotingComplete(gameId, round);

            // then
            assertThat(result).isFalse();
        }
    }
}
