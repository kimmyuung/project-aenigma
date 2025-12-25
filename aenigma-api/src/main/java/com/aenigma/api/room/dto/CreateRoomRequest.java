package com.aenigma.api.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {

    @NotBlank(message = "방 제목을 입력해주세요")
    @Size(min = 2, max = 50, message = "방 제목은 2~50자 사이여야 합니다")
    private String title;

    @Min(value = 2, message = "최소 2명 이상이어야 합니다")
    @Max(value = 10, message = "최대 10명까지 가능합니다")
    private Integer maxPlayers;

    @Size(max = 30, message = "비밀번호는 30자 이내여야 합니다")
    private String password;
}
