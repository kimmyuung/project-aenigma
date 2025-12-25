package com.aenigma.api.room.dto;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 방 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private String id;
    private String roomCode;
    private String title;
    private RoomStatus status;
    private int currentPlayers;
    private int maxPlayers;
    private boolean isPrivate;
    private HostInfo host;
    private List<MemberInfo> members;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HostInfo {
        private String id;
        private String nickname;
        private String displayTag;
        private String displayName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private String id;
        private String nickname;
        private String displayTag;
        private String displayName;
        private boolean isHost;
        private boolean isReady;
        private boolean isConnected;
    }

    /**
     * Entity -> DTO 변환
     */
    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .id(room.getId().toString())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .status(room.getStatus())
                .currentPlayers(room.getCurrentPlayerCount())
                .maxPlayers(room.getMaxPlayers())
                .isPrivate(room.isPrivate())
                .host(HostInfo.builder()
                        .id(room.getHost().getId().toString())
                        .nickname(room.getHost().getNickname())
                        .displayTag(room.getHost().getDisplayTag())
                        .displayName(room.getHost().getDisplayName())
                        .build())
                .members(room.getMembers().stream()
                        .map(member -> MemberInfo.builder()
                                .id(member.getUser().getId().toString())
                                .nickname(member.getUser().getNickname())
                                .displayTag(member.getUser().getDisplayTag())
                                .displayName(member.getUser().getDisplayName())
                                .isHost(member.getIsHost())
                                .isReady(member.getIsReady())
                                .isConnected(member.getIsConnected())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(room.getCreatedAt())
                .startedAt(room.getStartedAt())
                .build();
    }

    /**
     * 목록 조회용 간략 정보
     */
    public static RoomResponse listView(Room room) {
        return RoomResponse.builder()
                .id(room.getId().toString())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .status(room.getStatus())
                .currentPlayers(room.getCurrentPlayerCount())
                .maxPlayers(room.getMaxPlayers())
                .isPrivate(room.isPrivate())
                .host(HostInfo.builder()
                        .id(room.getHost().getId().toString())
                        .displayName(room.getHost().getDisplayName())
                        .build())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
