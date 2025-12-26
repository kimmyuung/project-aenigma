package com.aenigma.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 로그인/회원가입 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID userId;
    private String username;
    private String nickname;
    private String displayTag;
    private String displayName;
    private String accessToken;
    private String refreshToken;
}
