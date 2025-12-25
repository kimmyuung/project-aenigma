package com.aenigma.domain.entity;


import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomStatus;
import com.aenigma.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Room 엔티티 테스트")
class RoomTest {

    private User host;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .username("GUEST_TestHost")
                .nickname("방장")
                .displayTag("0001")
                .build();
    }

    @Nested
    @DisplayName("canJoin 메서드")
    class CanJoinTest {

        @Test
        @DisplayName("WAITING 상태이고 인원 여유가 있으면 true 반환")
        void canJoinWhenWaitingAndNotFull() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .maxPlayers(4)
                    .status(RoomStatus.WAITING)
                    .build();

            // when & then
            assertThat(room.canJoin()).isTrue();
        }

        @Test
        @DisplayName("PLAYING 상태이면 false 반환")
        void cannotJoinWhenPlaying() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.PLAYING)
                    .build();

            // when & then
            assertThat(room.canJoin()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPrivate 메서드")
    class IsPrivateTest {

        @Test
        @DisplayName("비밀번호가 있으면 true 반환")
        void privateRoomWithPassword() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("비밀방")
                    .host(host)
                    .password("secret123")
                    .build();

            // when & then
            assertThat(room.isPrivate()).isTrue();
        }

        @Test
        @DisplayName("비밀번호가 null이면 false 반환")
        void publicRoomWithNullPassword() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("공개방")
                    .host(host)
                    .password(null)
                    .build();

            // when & then
            assertThat(room.isPrivate()).isFalse();
        }

        @Test
        @DisplayName("비밀번호가 빈 문자열이면 false 반환")
        void publicRoomWithBlankPassword() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("공개방")
                    .host(host)
                    .password("   ")
                    .build();

            // when & then
            assertThat(room.isPrivate()).isFalse();
        }
    }

    @Nested
    @DisplayName("startGame 메서드")
    class StartGameTest {

        @Test
        @DisplayName("WAITING 상태에서 게임 시작 시 PLAYING으로 전환")
        void startsGameFromWaitingState() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.WAITING)
                    .build();

            // when
            room.startGame();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.PLAYING);
            assertThat(room.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("PLAYING 상태에서 게임 시작 시 예외 발생")
        void throwsExceptionWhenAlreadyPlaying() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.PLAYING)
                    .build();

            // when & then
            assertThatThrownBy(room::startGame)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("대기 상태에서만");
        }
    }

    @Nested
    @DisplayName("finishGame 메서드")
    class FinishGameTest {

        @Test
        @DisplayName("PLAYING 상태에서 게임 종료 시 FINISHED로 전환")
        void finishesGameFromPlayingState() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.PLAYING)
                    .build();

            // when
            room.finishGame();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
            assertThat(room.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("WAITING 상태에서 게임 종료 시 예외 발생")
        void throwsExceptionWhenNotPlaying() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.WAITING)
                    .build();

            // when & then
            assertThatThrownBy(room::finishGame)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("진행 중인 게임만");
        }
    }

    @Nested
    @DisplayName("close 메서드")
    class CloseTest {

        @Test
        @DisplayName("방 폐쇄 시 CLOSED 상태로 전환")
        void closesRoom() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .status(RoomStatus.WAITING)
                    .build();

            // when
            room.close();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("changeHost 메서드")
    class ChangeHostTest {

        @Test
        @DisplayName("방장 변경")
        void changesHost() {
            // given
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트 방")
                    .host(host)
                    .build();

            User newHost = User.builder()
                    .username("GUEST_NewHost")
                    .nickname("새방장")
                    .build();

            // when
            room.changeHost(newHost);

            // then
            assertThat(room.getHost()).isEqualTo(newHost);
        }
    }

    @Nested
    @DisplayName("Builder 기본값")
    class BuilderDefaultsTest {

        @Test
        @DisplayName("status 기본값은 WAITING")
        void defaultStatusIsWaiting() {
            // given & when
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트")
                    .host(host)
                    .build();

            // then
            assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        }

        @Test
        @DisplayName("maxPlayers 기본값은 6")
        void defaultMaxPlayersIsSix() {
            // given & when
            Room room = Room.builder()
                    .roomCode("ABC123")
                    .title("테스트")
                    .host(host)
                    .build();

            // then
            assertThat(room.getMaxPlayers()).isEqualTo(6);
        }
    }
}
