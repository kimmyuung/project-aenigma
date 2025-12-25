package com.aenigma.api.controller;

import com.aenigma.api.config.jwt.JwtProperties;
import com.aenigma.api.config.jwt.JwtProvider;
import com.aenigma.api.dto.GuestLoginRequest;
import com.aenigma.api.dto.LoginResponse;
import com.aenigma.api.dto.RefreshTokenRequest;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "인증 API")
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    /**
     * 게스트 로그인
     * 새로운 게스트 사용자를 생성하고 JWT 토큰을 반환
     */
    @PostMapping("/guest")
    @Operation(summary = "게스트 로그인", description = "닉네임으로 게스트 계정을 생성하고 로그인합니다")
    public ResponseEntity<LoginResponse> guestLogin(@Valid @RequestBody GuestLoginRequest request) {
        log.info("게스트 로그인 요청: {}", request.getNickname());

        // 게스트 사용자 생성
        User user = userService.createGuestUser(request.getNickname());

        // JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        // 응답 생성
        LoginResponse response = buildLoginResponse(user, accessToken, refreshToken);

        log.info("게스트 로그인 성공: {} ({})", user.getDisplayName(), user.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 기존 사용자 로그인 (username으로)
     */
    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "username으로 기존 계정에 로그인합니다")
    public ResponseEntity<LoginResponse> login(@RequestParam String username) {
        log.info("로그인 요청: {}", username);

        User user = userService.login(username);

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        LoginResponse response = buildLoginResponse(user, accessToken, refreshToken);

        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급합니다")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Refresh Token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().build();
        }

        // 토큰 타입 확인
        String tokenType = jwtProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            return ResponseEntity.badRequest().build();
        }

        // 사용자 조회
        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 새 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(user);
        String newRefreshToken = jwtProvider.createRefreshToken(user);

        LoginResponse response = buildLoginResponse(user, newAccessToken, newRefreshToken);

        return ResponseEntity.ok(response);
    }

    /**
     * LoginResponse 빌드 헬퍼
     */
    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000) // 초 단위
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .displayTag(user.getDisplayTag())
                        .displayName(user.getDisplayName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
