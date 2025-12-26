package com.aenigma.domain.entity;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.entity.VoteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Vote 엔티티 테스트")
class VoteTest {

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

        game = Game.builder()
                .id(UUID.randomUUID())
                .room(room)
                .roundNumber(1)
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

        voter = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user1)
                .role(GameRole.DETECTIVE)
                .isAlive(true)
                .build();

        target = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(game)
                .user(user2)
                .role(GameRole.CRIMINAL)
                .isAlive(true)
                .build();
    }

    @Test
    @DisplayName("Vote.create 팩토리 메서드로 투표 생성")
    void createsVoteSuccessfully() {
        // given
        int round = 1;
        VoteType voteType = VoteType.FINAL_VOTE;

        // when
        Vote vote = Vote.create(game, voter, target, voteType, round);

        // then
        assertThat(vote.getGame()).isEqualTo(game);
        assertThat(vote.getVoter()).isEqualTo(voter);
        assertThat(vote.getTarget()).isEqualTo(target);
        assertThat(vote.getVoteType()).isEqualTo(VoteType.FINAL_VOTE);
        assertThat(vote.getRound()).isEqualTo(round);
    }

    @Test
    @DisplayName("다양한 VoteType으로 투표 생성")
    void createsVoteWithDifferentTypes() {
        // given
        int round = 2;

        // when
        Vote eliminationVote = Vote.create(game, voter, target, VoteType.ELIMINATION_VOTE, round);

        // then
        assertThat(eliminationVote.getVoteType()).isEqualTo(VoteType.ELIMINATION_VOTE);
        assertThat(eliminationVote.getRound()).isEqualTo(2);
    }
}
