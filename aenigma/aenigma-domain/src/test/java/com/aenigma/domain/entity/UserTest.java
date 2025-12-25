package com.aenigma.domain.entity;

import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Nested
    @DisplayName("getDisplayName 메서드")
    class GetDisplayNameTest {

        @Test
        @DisplayName("displayTag가 있으면 '닉네임#태그' 형식 반환")
        void withDisplayTag() {
            // given
            User user = User.builder()
                    .nickname("탐정 김명호")
                    .displayTag("1234")
                    .build();

            // when
            String displayName = user.getDisplayName();

            // then
            assertThat(displayName).isEqualTo("탐정 김명호#1234");
        }

        @Test
        @DisplayName("displayTag가 null이면 닉네임만 반환")
        void withNullDisplayTag() {
            // given
            User user = User.builder()
                    .nickname("탐정 김명호")
                    .displayTag(null)
                    .build();

            // when
            String displayName = user.getDisplayName();

            // then
            assertThat(displayName).isEqualTo("탐정 김명호");
        }

        @Test
        @DisplayName("displayTag가 빈 문자열이면 닉네임만 반환")
        void withBlankDisplayTag() {
            // given
            User user = User.builder()
                    .nickname("탐정 김명호")
                    .displayTag("   ")
                    .build();

            // when
            String displayName = user.getDisplayName();

            // then
            assertThat(displayName).isEqualTo("탐정 김명호");
        }
    }

    @Nested
    @DisplayName("updateLastLogin 메서드")
    class UpdateLastLoginTest {

        @Test
        @DisplayName("마지막 로그인 시간이 갱신된다")
        void updatesLastLoginTime() {
            // given
            User user = User.builder()
                    .nickname("테스트유저")
                    .build();
            assertThat(user.getLastLoginAt()).isNull();

            // when
            user.updateLastLogin();

            // then
            assertThat(user.getLastLoginAt()).isNotNull();
            assertThat(user.getLastLoginAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("changeNickname 메서드")
    class ChangeNicknameTest {

        @Test
        @DisplayName("닉네임과 태그가 변경된다")
        void changesNicknameAndTag() {
            // given
            User user = User.builder()
                    .nickname("원래닉네임")
                    .displayTag("0000")
                    .build();

            // when
            user.changeNickname("새닉네임", "9999");

            // then
            assertThat(user.getNickname()).isEqualTo("새닉네임");
            assertThat(user.getDisplayTag()).isEqualTo("9999");
        }
    }

    @Nested
    @DisplayName("activate/deactivate 메서드")
    class ActivationTest {

        @Test
        @DisplayName("deactivate 호출 시 isActive가 false가 된다")
        void deactivateSetsIsActiveFalse() {
            // given
            User user = User.builder()
                    .nickname("테스트")
                    .isActive(true)
                    .build();

            // when
            user.deactivate();

            // then
            assertThat(user.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("activate 호출 시 isActive가 true가 된다")
        void activateSetsIsActiveTrue() {
            // given
            User user = User.builder()
                    .nickname("테스트")
                    .isActive(false)
                    .build();

            // when
            user.activate();

            // then
            assertThat(user.getIsActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultsTest {

        @Test
        @DisplayName("role 기본값은 GUEST")
        void defaultRoleIsGuest() {
            // given & when
            User user = User.builder()
                    .nickname("테스트")
                    .build();

            // then
            assertThat(user.getRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        @DisplayName("isActive 기본값은 true")
        void defaultIsActiveIsTrue() {
            // given & when
            User user = User.builder()
                    .nickname("테스트")
                    .build();

            // then
            assertThat(user.getIsActive()).isTrue();
        }
    }
}
