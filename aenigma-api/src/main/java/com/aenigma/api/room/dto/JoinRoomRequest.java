package com.aenigma.api.room.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방 입장 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {

    @Size(max = 30, message = "비밀번호는 30자 이내여야 합니다")
    private String password;
}
