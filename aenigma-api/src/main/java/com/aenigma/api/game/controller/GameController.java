package com.aenigma.api.game.controller;

import com.aenigma.api.config.DiscordConfig;
import com.aenigma.api.game.dto.ClueResponse;
import com.aenigma.api.game.dto.GameResponse;
import com.aenigma.api.game.dto.RoleDetailResponse;
import com.aenigma.api.game.dto.VoteRequest;
import com.aenigma.api.global.resolver.CurrentUserId;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GameClue;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 API Controller
 */
@Tag(name = "Game", description = "게임 API")
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final RoomService roomService;
    private final VoteService voteService;
    private final DiscordConfig discordConfig;

    /**
     * Discord 초대 링크 조회 (활성화 시에만 반환)
     */
    private String getDiscordInviteLink() {
        if (discordConfig.getBot().isEnabled()) {
            return discordConfig.getBot().getInviteLink();
        }
        return null;
    }

    /**
     * 게임 생성 및 역할 배정
     */
    @Operation(summary = "게임 생성", description = "방에서 새 게임을 생성하고 역할을 배정합니다. 시나리오 ID가 있으면 시나리오 기반으로 생성됩니다.")
    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @CurrentUserId UUID userId,
            @RequestParam UUID roomId,
            @RequestParam(required = false) UUID scenarioId) {

        Room room = roomService.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // 방장만 게임 생성 가능
        if (!room.getHost().getId().equals(userId)) {
            throw new IllegalStateException("방장만 게임을 생성할 수 있습니다.");
        }

        Game game = gameService.createAndStartGame(room, scenarioId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GameResponse.from(game, userId, getDiscordInviteLink()));
    }

    /**
     * 게임 시작
     */
    @Operation(summary = "게임 시작", description = "게임을 시작합니다.")
    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameResponse> startGame(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId) {

        Game game = gameService.startGame(gameId);
        return ResponseEntity.ok(GameResponse.from(game, userId, getDiscordInviteLink()));
    }

    /**
     * 다음 단계로 진행
     */
    @Operation(summary = "다음 단계", description = "게임을 다음 단계로 진행합니다.")
    @PostMapping("/{gameId}/next-phase")
    public ResponseEntity<GameResponse> nextPhase(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId) {

        Game game = gameService.nextPhase(gameId);
        return ResponseEntity.ok(GameResponse.from(game, userId));
    }

    /**
     * 플레이어 제거
     */
    @Operation(summary = "플레이어 제거", description = "투표 결과로 플레이어를 제거합니다.")
    @PostMapping("/{gameId}/eliminate/{targetUserId}")
    public ResponseEntity<GameResponse> eliminatePlayer(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId,
            @PathVariable UUID targetUserId) {

        gameService.eliminatePlayer(gameId, targetUserId);

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        return ResponseEntity.ok(GameResponse.from(game, userId));
    }

    /**
     * 게임 상세 조회
     */
    @Operation(summary = "게임 조회", description = "게임 상세 정보를 조회합니다.")
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId) {

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        return ResponseEntity.ok(GameResponse.from(game, userId, getDiscordInviteLink()));
    }

    /**
     * 내 단서 목록 조회
     */
    @Operation(summary = "내 단서 조회", description = "내가 볼 수 있는 단서 목록을 조회합니다.")
    @GetMapping("/{gameId}/clues")
    public ResponseEntity<List<ClueResponse>> getMyClues(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId) {

        List<GameClue> clues = gameService.getVisibleClues(gameId, userId);
        List<ClueResponse> response = clues.stream()
                .map(ClueResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 내 역할 상세 정보 조회
     */
    @Operation(summary = "내 역할 조회", description = "내 역할 상세 정보를 조회합니다.")
    @GetMapping("/{gameId}/my-role")
    public ResponseEntity<RoleDetailResponse> getMyRole(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId) {

        GamePlayer player = gameService.getPlayerRole(gameId, userId);
        return ResponseEntity.ok(RoleDetailResponse.from(player));
    }

    /**
     * 투표 제출
     */
    @Operation(summary = "투표", description = "최종 투표에서 범인을 지목합니다.")
    @PostMapping("/{gameId}/vote")
    public ResponseEntity<Map<String, Object>> vote(
            @CurrentUserId UUID userId,
            @PathVariable UUID gameId,
            @Valid @RequestBody VoteRequest request) {

        voteService.vote(gameId, userId, request.getTargetPlayerId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "투표가 완료되었습니다."));
    }

    /**
     * 방의 진행 중인 게임 조회
     */
    @Operation(summary = "진행 중인 게임 조회", description = "방의 진행 중인 게임을 조회합니다.")
    @GetMapping("/room/{roomId}/active")
    public ResponseEntity<GameResponse> getActiveGame(
            @CurrentUserId UUID userId,
            @PathVariable UUID roomId) {

        Game game = gameService.findActiveGameByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 게임이 없습니다."));

        return ResponseEntity.ok(GameResponse.from(game, userId, getDiscordInviteLink()));
    }

    /**
     * 사용자 게임 통계 조회
     */
    @Operation(summary = "게임 통계", description = "사용자의 게임 통계를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @CurrentUserId UUID userId) {

        Map<String, Object> stats = gameService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}
