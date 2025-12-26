package com.aenigma.api.room.controller;

import com.aenigma.api.room.dto.*;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.entity.RoomStatus;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 방 API Controller
 */
@Tag(name = "Room", description = "게임 방 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;

    /**
     * 방 생성
     */
    @Operation(summary = "방 생성", description = "새로운 게임 방을 생성합니다.")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateRoomRequest request) {

        User host = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Room room = roomService.createRoom(
                host,
                request.getTitle(),
                request.getMaxPlayers(),
                request.getPassword());

        return ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room));
    }

    /**
     * 방 입장
     */
    @Operation(summary = "방 입장", description = "방 코드로 게임 방에 입장합니다.")
    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody JoinRoomRequest request) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RoomMember member = roomService.joinRoom(
                request.getRoomCode(),
                user,
                request.getPassword());

        return ResponseEntity.ok(RoomResponse.from(member.getRoom()));
    }

    /**
     * 방 퇴장
     */
    @Operation(summary = "방 퇴장", description = "현재 참여 중인 방에서 퇴장합니다.")
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {

        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        roomService.leaveRoom(roomId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게임 시작 (방장만)
     */
    @Operation(summary = "게임 시작", description = "방장이 게임을 시작합니다.")
    @PostMapping("/{roomId}/start")
    public ResponseEntity<Void> startGame(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID roomId) {

        User host = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        roomService.startGame(roomId, host);
        return ResponseEntity.ok().build();
    }

    /**
     * 방 상세 조회
     */
    @Operation(summary = "방 상세 조회", description = "방 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable UUID roomId) {
        Room room = roomService.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        return ResponseEntity.ok(RoomResponse.from(room));
    }

    /**
     * 방 코드로 조회
     */
    @Operation(summary = "방 코드로 조회", description = "방 코드로 방 정보를 조회합니다.")
    @GetMapping("/code/{roomCode}")
    public ResponseEntity<RoomResponse> getRoomByCode(@PathVariable String roomCode) {
        Room room = roomService.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        return ResponseEntity.ok(RoomResponse.from(room));
    }

    /**
     * 입장 가능한 방 목록 조회
     */
    @Operation(summary = "방 목록 조회", description = "입장 가능한 방 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<RoomListResponse>> getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Boolean isPublic) {

        List<Room> rooms = roomService.searchRooms(keyword, status, isPublic);
        List<RoomListResponse> response = rooms.stream()
                .map(RoomListResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 입장 가능한 방 목록 (빠른 매칭용)
     */
    @Operation(summary = "빠른 매칭", description = "입장 가능한 방 목록을 조회합니다.")
    @GetMapping("/joinable")
    public ResponseEntity<List<RoomListResponse>> getJoinableRooms() {
        List<Room> rooms = roomService.findJoinableRooms();
        List<RoomListResponse> response = rooms.stream()
                .map(RoomListResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
