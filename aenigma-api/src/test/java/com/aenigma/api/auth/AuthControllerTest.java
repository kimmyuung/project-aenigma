package com.aenigma.api.auth;

import com.aenigma.api.auth.controller.AuthController;
import com.aenigma.api.auth.dto.LoginRequest;
import com.aenigma.api.auth.dto.RegisterRequest;
import com.aenigma.api.config.jwt.JwtProvider;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import com.aenigma.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("guest_12345")
                .nickname("테스터")
                .displayTag("1234")
                .role(UserRole.GUEST)
                .build();
    }

    @Nested
    @DisplayName("회원가입 API")
    class Register {

        @Test
        @WithMockUser
        @DisplayName("성공적으로 게스트 계정을 생성한다")
        void success() throws Exception {
            // given
            RegisterRequest request = new RegisterRequest();
            setField(request, "nickname", "테스터");

            given(userService.createGuestUser("테스터")).willReturn(testUser);
            given(jwtProvider.createAccessToken(any(User.class))).willReturn("mock-access-token");
            given(jwtProvider.createRefreshToken(any(User.class))).willReturn("mock-refresh-token");

            // when & then
            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                    .andExpect(jsonPath("$.nickname").value("테스터"))
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));
        }

        @Test
        @WithMockUser
        @DisplayName("닉네임이 비어있으면 실패한다")
        void failWithEmptyNickname() throws Exception {
            // given
            RegisterRequest request = new RegisterRequest();
            setField(request, "nickname", "");

            // when & then
            mockMvc.perform(post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("로그인 API")
    class Login {

        @Test
        @WithMockUser
        @DisplayName("성공적으로 로그인한다")
        void success() throws Exception {
            // given
            LoginRequest request = new LoginRequest();
            setField(request, "username", "guest_12345");

            given(userService.login("guest_12345")).willReturn(testUser);
            given(jwtProvider.createAccessToken(any(User.class))).willReturn("mock-access-token");
            given(jwtProvider.createRefreshToken(any(User.class))).willReturn("mock-refresh-token");

            // when & then
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"));
        }
    }

    @Nested
    @DisplayName("토큰 갱신 API")
    class Refresh {

        @Test
        @WithMockUser
        @DisplayName("유효한 refresh token으로 새 access token을 발급받는다")
        void success() throws Exception {
            // given
            String refreshToken = "valid-refresh-token";

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getTokenType(refreshToken)).willReturn("REFRESH");
            given(jwtProvider.getUsernameFromToken(refreshToken)).willReturn("guest_12345");
            given(userService.findByUsername("guest_12345")).willReturn(Optional.of(testUser));
            given(jwtProvider.createAccessToken(any(User.class))).willReturn("new-access-token");

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                    .with(csrf())
                    .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @WithMockUser
        @DisplayName("유효하지 않은 token이면 401을 반환한다")
        void failWithInvalidToken() throws Exception {
            // given
            String invalidToken = "invalid-token";
            given(jwtProvider.validateToken(invalidToken)).willReturn(false);

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                    .with(csrf())
                    .header("Authorization", "Bearer " + invalidToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("ACCESS 타입 토큰이면 401을 반환한다")
        void failWithAccessToken() throws Exception {
            // given
            String accessToken = "access-token";
            given(jwtProvider.validateToken(accessToken)).willReturn(true);
            given(jwtProvider.getTokenType(accessToken)).willReturn("ACCESS");

            // when & then
            mockMvc.perform(post("/api/auth/refresh")
                    .with(csrf())
                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // Reflection helper for setting private fields
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
