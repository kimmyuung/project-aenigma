package com.aenigma.api.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 방 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {

    @NotBlank(message = "방 제목은 필수입니다.")
    @Size(min = 2, max = 50, message = "방 제목은 2자 이상 50자 이하입니다.")
    private String title;

    @Min(value = 4, message = "최소 4명 이상이어야 합니다.")
    @Max(value = 10, message = "최대 10명까지 가능합니다.")
    private Integer maxPlayers;

    /**
     * 비밀방 비밀번호 (null이면 공개방)
     */
    private String password;
}
