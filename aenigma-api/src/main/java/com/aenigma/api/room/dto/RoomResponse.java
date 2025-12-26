package com.aenigma.api.room.dto;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 방 상세 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {

    private UUID id;
    private String roomCode;
    private String title;
    private RoomStatus status;
    private int currentPlayers;
    private int maxPlayers;
    private boolean isPrivate;
    private HostInfo host;
    private List<MemberInfo> members;
    private LocalDateTime createdAt;

    /**
     * 방장 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HostInfo {
        private UUID userId;
        private String nickname;
        private String displayName;
    }

    /**
     * 참여자 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private UUID memberId;
        private UUID userId;
        private String nickname;
        private String displayName;
        private boolean isHost;
        private boolean isReady;
    }

    public static RoomResponse from(Room room) {
        HostInfo hostInfo = HostInfo.builder()
                .userId(room.getHost().getId())
                .nickname(room.getHost().getNickname())
                .displayName(room.getHost().getDisplayName())
                .build();

        List<MemberInfo> memberInfos = room.getMembers().stream()
                .map(member -> MemberInfo.builder()
                        .memberId(member.getId())
                        .userId(member.getUser().getId())
                        .nickname(member.getUser().getNickname())
                        .displayName(member.getUser().getDisplayName())
                        .isHost(member.getIsHost())
                        .isReady(member.getIsReady())
                        .build())
                .toList();

        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .status(room.getStatus())
                .currentPlayers(room.getCurrentPlayerCount())
                .maxPlayers(room.getMaxPlayers())
                .isPrivate(room.isPrivate())
                .host(hostInfo)
                .members(memberInfos)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
