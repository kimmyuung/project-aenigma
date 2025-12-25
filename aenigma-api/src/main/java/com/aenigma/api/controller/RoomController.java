package com.aenigma.api.controller;

import com.aenigma.api.dto.CreateRoomRequest;
import com.aenigma.api.dto.JoinRoomRequest;
import com.aenigma.api.dto.RoomResponse;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.entity.RoomStatus;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 방 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room", description = "게임 방 API")
public class RoomController {

    private final RoomService roomService;

    /**
     * 방 생성
     */
    @PostMapping
    @Operation(summary = "방 생성", description = "새 게임 방을 생성합니다")
    public ResponseEntity<RoomResponse> createRoom(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRoomRequest request) {

        log.info("방 생성 요청: {} by {}", request.getTitle(), user.getDisplayName());

        Room room = roomService.createRoom(
                user,
                request.getTitle(),
                request.getMaxPlayers(),
                request.getPassword());

        return ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room));
    }

    /**
     * 방 목록 조회
     */
    @GetMapping
    @Operation(summary = "방 목록 조회", description = "입장 가능한 방 목록을 조회합니다")
    public ResponseEntity<List<RoomResponse>> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Boolean isPublic) {

        List<Room> rooms = roomService.searchRooms(keyword, status, isPublic);

        List<RoomResponse> response = rooms.stream()
                .map(RoomResponse::listView)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 입장 가능한 방 목록
     */
    @GetMapping("/joinable")
    @Operation(summary = "입장 가능한 방 목록", description = "현재 입장 가능한 방을 조회합니다")
    public ResponseEntity<List<RoomResponse>> getJoinableRooms() {
        List<Room> rooms = roomService.findJoinableRooms();

        List<RoomResponse> response = rooms.stream()
                .map(RoomResponse::listView)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 방 상세 조회
     */
    @GetMapping("/{roomId}")
    @Operation(summary = "방 상세 조회", description = "특정 방의 상세 정보를 조회합니다")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable UUID roomId) {
        Room room = roomService.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다"));

        return ResponseEntity.ok(RoomResponse.from(room));
    }

    /**
     * 코드로 방 조회
     */
    @GetMapping("/code/{roomCode}")
    @Operation(summary = "코드로 방 조회", description = "방 코드로 방 정보를 조회합니다")
    public ResponseEntity<RoomResponse> getRoomByCode(@PathVariable String roomCode) {
        Room room = roomService.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다"));

        return ResponseEntity.ok(RoomResponse.listView(room));
    }

    /**
     * 방 입장
     */
    @PostMapping("/code/{roomCode}/join")
    @Operation(summary = "방 입장", description = "방 코드로 게임 방에 입장합니다")
    public ResponseEntity<RoomResponse> joinRoom(
            @AuthenticationPrincipal User user,
            @PathVariable String roomCode,
            @RequestBody(required = false) JoinRoomRequest request) {

        String password = request != null ? request.getPassword() : null;

        log.info("방 입장 요청: {} -> {}", user.getDisplayName(), roomCode);

        RoomMember member = roomService.joinRoom(roomCode, user, password);
        Room room = member.getRoom();

        return ResponseEntity.ok(RoomResponse.from(room));
    }

    /**
     * 방 퇴장
     */
    @PostMapping("/{roomId}/leave")
    @Operation(summary = "방 퇴장", description = "현재 참여 중인 방에서 퇴장합니다")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal User user,
            @PathVariable UUID roomId) {

        log.info("방 퇴장 요청: {} <- {}", user.getDisplayName(), roomId);

        roomService.leaveRoom(roomId, user);

        return ResponseEntity.noContent().build();
    }

    /**
     * 게임 시작
     */
    @PostMapping("/{roomId}/start")
    @Operation(summary = "게임 시작", description = "방장이 게임을 시작합니다")
    public ResponseEntity<Void> startGame(
            @AuthenticationPrincipal User user,
            @PathVariable UUID roomId) {

        log.info("게임 시작 요청: {} by {}", roomId, user.getDisplayName());

        roomService.startGame(roomId, user);

        return ResponseEntity.ok().build();
    }
}
