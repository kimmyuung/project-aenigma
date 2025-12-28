package com.aenigma.api.game.controller;

import com.aenigma.api.config.DiscordConfig;
import com.aenigma.api.game.dto.VoteRequest;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GameClue;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.vote.entity.Vote;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GameController 단위 테스트
 */
@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    @MockBean
    private RoomService roomService;

    @MockBean
    private VoteService voteService;

    @MockBean
    private DiscordConfig discordConfig;

    @MockBean
    private DiscordConfig.BotConfig botConfig;

    private UUID userId;
    private UUID gameId;
    private UUID roomId;
    private Game mockGame;
    private Room mockRoom;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        mockUser = User.builder()
                .id(userId)
                .nickname("테스트유저")
                .build();

        mockRoom = Room.builder()
                .id(roomId)
                .title("테스트 방")
                .host(mockUser)
                .build();

        mockGame = Game.builder()
                .id(gameId)
                .room(mockRoom)
                .roundNumber(1)
                .phase(GamePhase.INTRO)
                .build();

        // Discord Config Mock
        given(discordConfig.getBot()).willReturn(botConfig);
        given(botConfig.isEnabled()).willReturn(false);
    }

    @Test
    @DisplayName("게임 조회 성공")
    @WithMockUser
    void getGame_Success() throws Exception {
        // given
        given(gameService.findById(gameId)).willReturn(Optional.of(mockGame));

        // when & then
        mockMvc.perform(get("/api/games/{gameId}", gameId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    @Test
    @DisplayName("게임 조회 실패 - 게임 없음")
    @WithMockUser
    void getGame_NotFound() throws Exception {
        // given
        given(gameService.findById(gameId)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/games/{gameId}", gameId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게임 생성 성공")
    @WithMockUser
    void createGame_Success() throws Exception {
        // given
        given(roomService.findById(roomId)).willReturn(Optional.of(mockRoom));
        given(gameService.createAndStartGame(any(Room.class), eq(null))).willReturn(mockGame);

        // when & then
        mockMvc.perform(post("/api/games")
                .header("X-User-Id", userId.toString())
                .param("roomId", roomId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    @Test
    @DisplayName("게임 생성 실패 - 방장 아님")
    @WithMockUser
    void createGame_NotHost() throws Exception {
        // given
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .nickname("다른유저")
                .build();

        Room roomWithOtherHost = Room.builder()
                .id(roomId)
                .title("테스트 방")
                .host(otherUser)
                .build();

        given(roomService.findById(roomId)).willReturn(Optional.of(roomWithOtherHost));

        // when & then
        mockMvc.perform(post("/api/games")
                .header("X-User-Id", userId.toString())
                .param("roomId", roomId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게임 시작 성공")
    @WithMockUser
    void startGame_Success() throws Exception {
        // given
        Game startedGame = Game.builder()
                .id(gameId)
                .room(mockRoom)
                .roundNumber(1)
                .phase(GamePhase.LOBBY)
                .build();

        given(gameService.startGame(gameId)).willReturn(startedGame);

        // when & then
        mockMvc.perform(post("/api/games/{gameId}/start", gameId)
                .header("X-User-Id", userId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("LOBBY"));
    }

    @Test
    @DisplayName("내 역할 조회 성공")
    @WithMockUser
    void getMyRole_Success() throws Exception {
        // given
        GamePlayer mockPlayer = GamePlayer.builder()
                .id(UUID.randomUUID())
                .game(mockGame)
                .user(mockUser)
                .role(GameRole.DETECTIVE)
                .isAlive(true)
                .build();

        given(gameService.getPlayerRole(gameId, userId)).willReturn(mockPlayer);

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/my-role", gameId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DETECTIVE"))
                .andExpect(jsonPath("$.isAlive").value(true));
    }

    @Test
    @DisplayName("단서 목록 조회 성공")
    @WithMockUser
    void getMyClues_Success() throws Exception {
        // given
        given(gameService.getVisibleClues(gameId, userId)).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/games/{gameId}/clues", gameId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("투표 성공")
    @WithMockUser
    void vote_Success() throws Exception {
        // given
        UUID targetPlayerId = UUID.randomUUID();
        VoteRequest request = VoteRequest.builder()
                .targetPlayerId(targetPlayerId)
                .build();

        // when & then
        mockMvc.perform(post("/api/games/{gameId}/vote", gameId)
                .header("X-User-Id", userId.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
