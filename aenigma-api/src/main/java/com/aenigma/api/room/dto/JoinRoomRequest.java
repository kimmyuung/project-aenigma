package com.aenigma.api.room.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 방 입장 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRoomRequest {

    @NotBlank(message = "방 코드는 필수입니다.")
    private String roomCode;

    /**
     * 비밀방 비밀번호 (공개방이면 null)
     */
    private String password;
}
