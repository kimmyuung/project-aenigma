package com.aenigma.api.auth.controller;

import com.aenigma.api.auth.dto.AuthResponse;
import com.aenigma.api.auth.dto.LoginRequest;
import com.aenigma.api.auth.dto.RegisterRequest;
import com.aenigma.api.config.jwt.JwtProvider;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API Controller
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    /**
     * 게스트 회원가입
     */
    @Operation(summary = "게스트 회원가입", description = "닉네임으로 게스트 계정을 생성합니다.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createGuestUser(request.getNickname());

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .displayTag(user.getDisplayTag())
                .displayName(user.getDisplayName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게스트 로그인 (기존 계정)
     */
    @Operation(summary = "게스트 로그인", description = "기존 게스트 계정으로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername());

        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .displayTag(user.getDisplayTag())
                .displayName(user.getDisplayName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.replace("Bearer ", "");

        if (!jwtProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String tokenType = jwtProvider.getTokenType(token);
        if (!"REFRESH".equals(tokenType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtProvider.getUsernameFromToken(token);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtProvider.createAccessToken(user);

        AuthResponse response = AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .displayTag(user.getDisplayTag())
                .displayName(user.getDisplayName())
                .accessToken(newAccessToken)
                .refreshToken(token) // 기존 refresh token 유지
                .build();

        return ResponseEntity.ok(response);
    }
}
