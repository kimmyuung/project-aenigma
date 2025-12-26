package com.aenigma.api.room.dto;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 방 목록 응답 DTO (간략 정보)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomListResponse {

    private UUID id;
    private String roomCode;
    private String title;
    private RoomStatus status;
    private int currentPlayers;
    private int maxPlayers;
    private boolean isPrivate;
    private String hostNickname;

    public static RoomListResponse from(Room room) {
        return RoomListResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .title(room.getTitle())
                .status(room.getStatus())
                .currentPlayers(room.getCurrentPlayerCount())
                .maxPlayers(room.getMaxPlayers())
                .isPrivate(room.isPrivate())
                .hostNickname(room.getHost().getNickname())
                .build();
    }
}
