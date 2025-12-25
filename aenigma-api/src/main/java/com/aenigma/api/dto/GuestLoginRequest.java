package com.aenigma.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게스트 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestLoginRequest {

    /**
     * 사용자가 입력한 닉네임
     */
    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(min = 2, max = 30, message = "닉네임은 2~30자 사이여야 합니다")
    private String nickname;
}
