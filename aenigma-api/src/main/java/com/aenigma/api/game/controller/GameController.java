package com.aenigma.api.game.controller;

import com.aenigma.api.game.dto.GameResponse;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    /**
     * 게임 생성 및 역할 배정
     */
    @Operation(summary = "게임 생성", description = "방에서 새 게임을 생성하고 역할을 배정합니다.")
    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam UUID roomId) {

        Room room = roomService.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // 방장만 게임 생성 가능
        if (!room.getHost().getId().equals(userId)) {
            throw new IllegalStateException("방장만 게임을 생성할 수 있습니다.");
        }

        // 게임 생성
        Game game = gameService.createGame(room);

        // 역할 배정
        List<GamePlayer> players = gameService.assignRoles(game, room.getMembers());

        return ResponseEntity.status(HttpStatus.CREATED).body(GameResponse.from(game, userId));
    }

    /**
     * 게임 시작
     */
    @Operation(summary = "게임 시작", description = "게임을 시작합니다.")
    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameResponse> startGame(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId) {

        Game game = gameService.startGame(gameId);
        return ResponseEntity.ok(GameResponse.from(game, userId));
    }

    /**
     * 다음 단계로 진행
     */
    @Operation(summary = "다음 단계", description = "게임을 다음 단계로 진행합니다.")
    @PostMapping("/{gameId}/next-phase")
    public ResponseEntity<GameResponse> nextPhase(
            @RequestHeader("X-User-Id") UUID userId,
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
            @RequestHeader("X-User-Id") UUID userId,
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
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID gameId) {

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        return ResponseEntity.ok(GameResponse.from(game, userId));
    }

    /**
     * 방의 진행 중인 게임 조회
     */
    @Operation(summary = "진행 중인 게임 조회", description = "방의 진행 중인 게임을 조회합니다.")
    @GetMapping("/room/{roomId}/active")
    public ResponseEntity<GameResponse> getActiveGame(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {

        Game game = gameService.findActiveGameByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 게임이 없습니다."));

        return ResponseEntity.ok(GameResponse.from(game, userId));
    }

    /**
     * 사용자 게임 통계 조회
     */
    @Operation(summary = "게임 통계", description = "사용자의 게임 통계를 조회합니다.")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @RequestHeader("X-User-Id") UUID userId) {

        Map<String, Object> stats = gameService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}
