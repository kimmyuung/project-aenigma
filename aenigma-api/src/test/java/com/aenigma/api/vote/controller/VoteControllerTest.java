package com.aenigma.api.vote.controller;

import com.aenigma.api.game.dto.VoteRequest;
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
import com.aenigma.domain.vote.service.VoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VoteController 단위 테스트
 */
@WebMvcTest(VoteController.class)
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoteService voteService;

    @MockBean
    private GameRepository gameRepository;

    @MockBean
    private GamePlayerRepository gamePlayerRepository;

    private UUID userId;
    private UUID gameId;
    private UUID targetPlayerId;
    private Game mockGame;
    private GamePlayer mockPlayer;
    private GamePlayer mockTarget;
    private Vote mockVote;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        targetPlayerId = UUID.randomUUID();

        User mockUser = User.builder()
                .id(userId)
                .nickname("투표자")
                .build();

        User targetUser = User.builder()
                .id(UUID.randomUUID())
                .nickname("투표대상")
                .build();

        Room mockRoom = Room.builder()
                .id(UUID.randomUUID())
                .title("테스트 방")
                .host(mockUser)
                .build();

        mockGame = Game.builder()
                .id(gameId)
                .room(mockRoom)
                .roundNumber(1)
                .phase(GamePhase.FINAL_VOTE)
                .investigationRound(1)
                .build();

        mockPlayer = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(mockGame)
                .user(mockUser)
                .role(GameRole.SUSPECT)
                .isAlive(true)
                .build();

        mockTarget = GamePlayer.builder()
                .id(targetPlayerId)
                .game(mockGame)
                .user(targetUser)
                .role(GameRole.CRIMINAL)
                .isAlive(true)
                .build();

        mockVote = Vote.builder()
                .id(UUID.randomUUID())
                .game(mockGame)
                .voter(mockPlayer)
                .target(mockTarget)
                .voteType(VoteType.FINAL_VOTE)
                .round(1)
                .build();
    }

    @Test
    @DisplayName("투표 제출 성공")
    @WithMockUser
    void castVote_Success() throws Exception {
        // given
        VoteRequest request = VoteRequest.builder()
                .targetPlayerId(targetPlayerId)
                .build();

        given(voteService.vote(eq(gameId), eq(userId), eq(targetPlayerId)))
                .willReturn(mockVote);

        // when & then
        mockMvc.perform(post("/api/games/{gameId}/votes", gameId)
                .header("X-User-Id", userId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voteType").value("FINAL_VOTE"))
                .andExpect(jsonPath("$.round").value(1));
    }

    @Test
    @DisplayName("투표 결과 조회 성공")
    @WithMockUser
    void getVoteResults_Success() throws Exception {
        // given
        Map<UUID, Long> results = new LinkedHashMap<>();
        results.put(targetPlayerId, 3L);
        results.put(mockPlayer.getId(), 1L);

        mockGame.getPlayers().add(mockPlayer);
        mockGame.getPlayers().add(mockTarget);

        given(gameRepository.findById(gameId)).willReturn(Optional.of(mockGame));
        given(voteService.getVoteResults(gameId, 1)).willReturn(results);
        given(voteService.isVotingComplete(gameId, 1)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/votes/results", gameId)
                .header("X-User-Id", userId.toString())
                .param("round", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(1))
                .andExpect(jsonPath("$.isComplete").value(true))
                .andExpect(jsonPath("$.totalVotes").value(4));
    }

    @Test
    @DisplayName("투표 상태 조회 성공")
    @WithMockUser
    void getVoteStatus_Success() throws Exception {
        // given
        given(voteService.isVotingComplete(gameId, 1)).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/votes/status", gameId)
                .header("X-User-Id", userId.toString())
                .param("round", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.round").value(1))
                .andExpect(jsonPath("$.isComplete").value(false));
    }

    @Test
    @DisplayName("최다 득표자 조회 성공")
    @WithMockUser
    void getMostVotedPlayers_Success() throws Exception {
        // given
        List<GamePlayer> topPlayers = List.of(mockTarget);
        given(voteService.getMostVotedPlayers(gameId, 1)).willReturn(topPlayers);

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/votes/top", gameId)
                .header("X-User-Id", userId.toString())
                .param("round", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].playerId").value(targetPlayerId.toString()));
    }

    @Test
    @DisplayName("내 투표 기록 조회 성공")
    @WithMockUser
    void getMyVotes_Success() throws Exception {
        // given
        List<Vote> votes = List.of(mockVote);
        given(gamePlayerRepository.findByGameIdAndUserId(gameId, userId))
                .willReturn(Optional.of(mockPlayer));
        given(voteService.getPlayerVoteHistory(gameId, mockPlayer.getId()))
                .willReturn(votes);

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/votes/my", gameId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].round").value(1));
    }

    @Test
    @DisplayName("투표 제출 실패 - 대상 없음")
    @WithMockUser
    void castVote_NoTarget() throws Exception {
        // given
        VoteRequest request = VoteRequest.builder()
                .targetPlayerId(null) // 대상 없음
                .build();

        // when & then
        mockMvc.perform(post("/api/games/{gameId}/votes", gameId)
                .header("X-User-Id", userId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
