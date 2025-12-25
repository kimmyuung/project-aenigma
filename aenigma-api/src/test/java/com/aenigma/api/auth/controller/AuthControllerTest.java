package com.aenigma.api.auth.controller;

import com.aenigma.api.config.jwt.JwtProperties;
import com.aenigma.api.config.jwt.JwtProvider;
import com.aenigma.api.auth.dto.GuestLoginRequest;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

    @MockBean
    private JwtProperties jwtProperties;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_TestUser")
                .nickname("테스트유저")
                .displayTag("1234")
                .role(UserRole.GUEST)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/guest - 게스트 로그인")
    class GuestLoginTest {

        @Test
        @DisplayName("유효한 닉네임으로 게스트 로그인 성공")
        @WithMockUser
        void guestLoginSuccess() throws Exception {
            // given
            GuestLoginRequest request = GuestLoginRequest.builder()
                    .nickname("탐정 김명호")
                    .build();

            given(userService.createGuestUser(any())).willReturn(testUser);
            given(jwtProvider.createAccessToken(any())).willReturn("access-token");
            given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
            given(jwtProperties.getAccessTokenExpiration()).willReturn(3600000L);

            // when & then
            mockMvc.perform(post("/api/v1/auth/guest")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.nickname").value("테스트유저"));
        }

        @Test
        @DisplayName("닉네임 없이 요청 시 400 에러")
        @WithMockUser
        void guestLoginWithoutNickname() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(post("/api/v1/auth/guest")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("닉네임이 너무 짧으면 400 에러")
        @WithMockUser
        void guestLoginWithShortNickname() throws Exception {
            // given
            GuestLoginRequest request = GuestLoginRequest.builder()
                    .nickname("a") // 1글자 (최소 2글자)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/auth/guest")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - 기존 사용자 로그인")
    class LoginTest {

        @Test
        @DisplayName("유효한 username으로 로그인 성공")
        @WithMockUser
        void loginSuccess() throws Exception {
            // given
            String username = "GUEST_TestUser";

            given(userService.login(username)).willReturn(testUser);
            given(jwtProvider.createAccessToken(any())).willReturn("access-token");
            given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
            given(jwtProperties.getAccessTokenExpiration()).willReturn(3600000L);

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .param("username", username))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.user.username").value(username));
        }
    }
}
