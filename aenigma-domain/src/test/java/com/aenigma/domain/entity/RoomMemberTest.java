package com.aenigma.domain.entity;

import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoomMember 엔티티 테스트")
class RoomMemberTest {

    private User user;
    private Room room;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("GUEST_TestUser")
                .nickname("테스트유저")
                .displayTag("1234")
                .build();

        User host = User.builder()
                .username("GUEST_TestHost")
                .nickname("방장")
                .displayTag("0001")
                .build();

        room = Room.builder()
                .roomCode("ABC123")
                .title("테스트 방")
                .host(host)
                .build();
    }

    @Nested
    @DisplayName("toggleReady 메서드")
    class ToggleReadyTest {

        @Test
        @DisplayName("일반 멤버는 준비 상태를 토글할 수 있다")
        void toggleReadyForNormalMember() {
            // given
            RoomMember member = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .isHost(false)
                    .isReady(false)
                    .build();

            // when
            member.toggleReady();

            // then
            assertThat(member.getIsReady()).isTrue();

            // when (다시 토글)
            member.toggleReady();

            // then
            assertThat(member.getIsReady()).isFalse();
        }

        @Test
        @DisplayName("방장은 준비 상태를 토글할 수 없다")
        void hostCannotToggleReady() {
            // given
            RoomMember hostMember = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .isHost(true)
                    .isReady(true)
                    .build();

            // when
            hostMember.toggleReady();

            // then (변경 없음)
            assertThat(hostMember.getIsReady()).isTrue();
        }
    }

    @Nested
    @DisplayName("promoteToHost 메서드")
    class PromoteToHostTest {

        @Test
        @DisplayName("방장으로 임명 시 isHost=true, isReady=true")
        void promotesToHost() {
            // given
            RoomMember member = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .isHost(false)
                    .isReady(false)
                    .build();

            // when
            member.promoteToHost();

            // then
            assertThat(member.getIsHost()).isTrue();
            assertThat(member.getIsReady()).isTrue();
        }
    }

    @Nested
    @DisplayName("demoteFromHost 메서드")
    class DemoteFromHostTest {

        @Test
        @DisplayName("방장 해제 시 isHost=false, isReady=false")
        void demotesFromHost() {
            // given
            RoomMember hostMember = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .isHost(true)
                    .isReady(true)
                    .build();

            // when
            hostMember.demoteFromHost();

            // then
            assertThat(hostMember.getIsHost()).isFalse();
            assertThat(hostMember.getIsReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("setConnectionStatus 메서드")
    class SetConnectionStatusTest {

        @Test
        @DisplayName("연결 상태를 변경할 수 있다")
        void changesConnectionStatus() {
            // given
            RoomMember member = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .isConnected(true)
                    .build();

            // when
            member.setConnectionStatus(false);

            // then
            assertThat(member.getIsConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("정적 팩토리 메서드")
    class FactoryMethodTest {

        @Test
        @DisplayName("createMember - 일반 멤버 생성")
        void createMemberCreatesNormalMember() {
            // when
            RoomMember member = RoomMember.createMember(room, user);

            // then
            assertThat(member.getRoom()).isEqualTo(room);
            assertThat(member.getUser()).isEqualTo(user);
            assertThat(member.getIsHost()).isFalse();
            assertThat(member.getIsReady()).isFalse();
        }

        @Test
        @DisplayName("createHost - 방장 멤버 생성")
        void createHostCreatesHostMember() {
            // when
            RoomMember hostMember = RoomMember.createHost(room, user);

            // then
            assertThat(hostMember.getRoom()).isEqualTo(room);
            assertThat(hostMember.getUser()).isEqualTo(user);
            assertThat(hostMember.getIsHost()).isTrue();
            assertThat(hostMember.getIsReady()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultsTest {

        @Test
        @DisplayName("기본값 확인")
        void defaultValues() {
            // given & when
            RoomMember member = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .build();

            // then
            assertThat(member.getIsHost()).isFalse();
            assertThat(member.getIsReady()).isFalse();
            assertThat(member.getIsConnected()).isTrue();
        }
    }
}
