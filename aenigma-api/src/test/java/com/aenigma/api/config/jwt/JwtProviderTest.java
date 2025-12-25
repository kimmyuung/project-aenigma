package com.aenigma.api.config.jwt;

import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider 테스트")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        // JwtProperties 설정
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(Base64.getEncoder().encodeToString(
                "test-secret-key-for-jwt-testing-256-bits-long!!".getBytes()));
        jwtProperties.setAccessTokenExpiration(3600000L); // 1시간
        jwtProperties.setRefreshTokenExpiration(604800000L); // 7일
        jwtProperties.setIssuer("aenigma-test");

        jwtProvider = new JwtProvider(jwtProperties);
        jwtProvider.init();

        // 테스트 사용자
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_TestUser")
                .nickname("테스트유저")
                .displayTag("1234")
                .role(UserRole.GUEST)
                .build();
    }

    @Nested
    @DisplayName("토큰 생성")
    class TokenCreationTest {

        @Test
        @DisplayName("Access Token 생성 성공")
        void createsAccessTokenSuccessfully() {
            // when
            String token = jwtProvider.createAccessToken(testUser);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // JWT 구조 확인
        }

        @Test
        @DisplayName("Refresh Token 생성 성공")
        void createsRefreshTokenSuccessfully() {
            // when
            String token = jwtProvider.createRefreshToken(testUser);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Access Token과 Refresh Token은 서로 다르다")
        void accessAndRefreshTokensAreDifferent() {
            // when
            String accessToken = jwtProvider.createAccessToken(testUser);
            String refreshToken = jwtProvider.createRefreshToken(testUser);

            // then
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class TokenValidationTest {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validatesValidTokenSuccessfully() {
            // given
            String token = jwtProvider.createAccessToken(testUser);

            // when
            boolean isValid = jwtProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 토큰 검증 실패")
        void failsValidationForInvalidToken() {
            // given
            String invalidToken = "invalid.jwt.token";

            // when
            boolean isValid = jwtProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰 검증 실패")
        void failsValidationForEmptyToken() {
            // when
            boolean isValid = jwtProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null 토큰 검증 실패")
        void failsValidationForNullToken() {
            // when
            boolean isValid = jwtProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰에서 정보 추출")
    class TokenExtractionTest {

        @Test
        @DisplayName("토큰에서 사용자 ID 추출")
        void extractsUserIdFromToken() {
            // given
            String token = jwtProvider.createAccessToken(testUser);

            // when
            UUID extractedId = jwtProvider.getUserIdFromToken(token);

            // then
            assertThat(extractedId).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("토큰에서 username 추출")
        void extractsUsernameFromToken() {
            // given
            String token = jwtProvider.createAccessToken(testUser);

            // when
            String extractedUsername = jwtProvider.getUsernameFromToken(token);

            // then
            assertThat(extractedUsername).isEqualTo(testUser.getUsername());
        }

        @Test
        @DisplayName("Access Token의 타입은 ACCESS")
        void accessTokenTypeIsAccess() {
            // given
            String token = jwtProvider.createAccessToken(testUser);

            // when
            String tokenType = jwtProvider.getTokenType(token);

            // then
            assertThat(tokenType).isEqualTo("ACCESS");
        }

        @Test
        @DisplayName("Refresh Token의 타입은 REFRESH")
        void refreshTokenTypeIsRefresh() {
            // given
            String token = jwtProvider.createRefreshToken(testUser);

            // when
            String tokenType = jwtProvider.getTokenType(token);

            // then
            assertThat(tokenType).isEqualTo("REFRESH");
        }

        @Test
        @DisplayName("토큰 만료 시간 추출")
        void extractsExpirationFromToken() {
            // given
            String token = jwtProvider.createAccessToken(testUser);

            // when
            var expiration = jwtProvider.getExpirationFromToken(token);

            // then
            assertThat(expiration).isNotNull();
            assertThat(expiration.getTime()).isGreaterThan(System.currentTimeMillis());
        }
    }
}
