package com.aenigma.domain.service;


import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import com.aenigma.domain.user.repository.UserRepository;
import com.aenigma.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("createGuestUser 메서드")
    class CreateGuestUserTest {

        @Test
        @DisplayName("게스트 사용자 생성 성공")
        void createsGuestUserSuccessfully() {
            // given
            String nickname = "탐정 김명호";

            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(userRepository.findDisplayTagsByNickname(nickname)).willReturn(Collections.emptyList());
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                // ID 설정 시뮬레이션
                return User.builder()
                        .id(UUID.randomUUID())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .displayTag(user.getDisplayTag())
                        .role(user.getRole())
                        .build();
            });

            // when
            User result = userService.createGuestUser(nickname);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNickname()).isEqualTo(nickname);
            assertThat(result.getUsername()).startsWith("GUEST_");
            assertThat(result.getDisplayTag()).hasSize(4);
            assertThat(result.getRole()).isEqualTo(UserRole.GUEST);

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("findById 메서드")
    class FindByIdTest {

        @Test
        @DisplayName("ID로 사용자 조회 성공")
        void findsByIdSuccessfully() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("GUEST_Test1234")
                    .nickname("테스트")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            Optional<User> result = userService.findById(userId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional 반환")
        void returnsEmptyForNonExistentId() {
            // given
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            Optional<User> result = userService.findById(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("login 메서드")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 시 lastLoginAt 갱신")
        void updatesLastLoginOnSuccess() {
            // given
            String username = "GUEST_Test1234";
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username(username)
                    .nickname("테스트")
                    .build();

            given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

            // when
            User result = userService.login(username);

            // then
            assertThat(result.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 username 로그인 시 예외 발생")
        void throwsExceptionForNonExistentUsername() {
            // given
            String username = "GUEST_NotExist";
            given(userRepository.findByUsername(username)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.login(username))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("changeNickname 메서드")
    class ChangeNicknameTest {

        @Test
        @DisplayName("닉네임 변경 성공")
        void changesNicknameSuccessfully() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("GUEST_Test1234")
                    .nickname("원래닉네임")
                    .displayTag("0000")
                    .build();

            String newNickname = "새닉네임";

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.findDisplayTagsByNickname(newNickname)).willReturn(Collections.emptyList());

            // when
            User result = userService.changeNickname(userId, newNickname);

            // then
            assertThat(result.getNickname()).isEqualTo(newNickname);
            assertThat(result.getDisplayTag()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deactivateUser 메서드")
    class DeactivateUserTest {

        @Test
        @DisplayName("사용자 비활성화 성공")
        void deactivatesUserSuccessfully() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder()
                    .id(userId)
                    .username("GUEST_Test1234")
                    .nickname("테스트")
                    .isActive(true)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.deactivateUser(userId);

            // then
            assertThat(user.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 비활성화 시 예외 발생")
        void throwsExceptionForNonExistentUser() {
            // given
            UUID userId = UUID.randomUUID();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deactivateUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }
}
