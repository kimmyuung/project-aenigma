package com.aenigma.domain.service;

import com.aenigma.domain.entity.*;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.entity.RoomStatus;
import com.aenigma.domain.room.repository.RoomMemberRepository;
import com.aenigma.domain.room.repository.RoomRepository;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.entity.UserRole;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService 테스트")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private JPAQueryFactory queryFactory;

    @InjectMocks
    private RoomService roomService;

    private User host;
    private User player;

    @BeforeEach
    void setUp() {
        host = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_Host1234")
                .nickname("방장")
                .displayTag("0001")
                .role(UserRole.GUEST)
                .build();

        player = User.builder()
                .id(UUID.randomUUID())
                .username("GUEST_Play1234")
                .nickname("참가자")
                .displayTag("0002")
                .role(UserRole.GUEST)
                .build();
    }

    @Nested
    @DisplayName("createRoom 메서드")
    class CreateRoomTest {

        @Test
        @DisplayName("방 생성 성공")
        void createsRoomSuccessfully() {
            // given
            String title = "추리 게임방";
            Integer maxPlayers = 4;

            given(roomRepository.existsByRoomCode(anyString())).willReturn(false);
            given(roomRepository.save(any(Room.class))).willAnswer(invocation -> {
                Room room = invocation.getArgument(0);
                return Room.builder()
                        .id(UUID.randomUUID())
                        .roomCode(room.getRoomCode())
                        .title(room.getTitle())
                        .host(room.getHost())
                        .maxPlayers(room.getMaxPlayers())
                        .members(new ArrayList<>())
                        .build();
            });
            given(roomMemberRepository.save(any(RoomMember.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Room result = roomService.createRoom(host, title, maxPlayers, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(title);
            assertThat(result.getMaxPlayers()).isEqualTo(maxPlayers);
            assertThat(result.getRoomCode()).hasSize(6);

            verify(roomRepository).save(any(Room.class));
            verify(roomMemberRepository).save(any(RoomMember.class));
        }

        @Test
        @DisplayName("방 생성 시 기본 최대 인원은 6")
        void defaultMaxPlayersIsSix() {
            // given
            given(roomRepository.existsByRoomCode(anyString())).willReturn(false);
            given(roomRepository.save(any(Room.class))).willAnswer(invocation -> {
                Room room = invocation.getArgument(0);
                return Room.builder()
                        .id(UUID.randomUUID())
                        .roomCode(room.getRoomCode())
                        .title(room.getTitle())
                        .host(room.getHost())
                        .maxPlayers(room.getMaxPlayers())
                        .members(new ArrayList<>())
                        .build();
            });
            given(roomMemberRepository.save(any(RoomMember.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            Room result = roomService.createRoom(host, "테스트방", null, null);

            // then
            assertThat(result.getMaxPlayers()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("joinRoom 메서드")
    class JoinRoomTest {

        @Test
        @DisplayName("방 입장 실패 - 존재하지 않는 방")
        void failsWhenRoomNotFound() {
            // given
            given(roomRepository.findByRoomCode("ABCDEF")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.joinRoom("ABCDEF", player, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 방");
        }

        @Test
        @DisplayName("방 입장 실패 - 이미 참여 중")
        void failsWhenAlreadyJoined() {
            // given
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .roomCode("ABCDEF")
                    .title("테스트")
                    .host(host)
                    .status(RoomStatus.WAITING)
                    .maxPlayers(4)
                    .members(new ArrayList<>())
                    .build();

            given(roomRepository.findByRoomCode("ABCDEF")).willReturn(Optional.of(room));
            given(roomMemberRepository.existsByRoomAndUser(room, player)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> roomService.joinRoom("ABCDEF", player, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 참여 중");
        }

        @Test
        @DisplayName("방 입장 실패 - 비밀번호 불일치")
        void failsWhenPasswordMismatch() {
            // given
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .roomCode("ABCDEF")
                    .title("비밀방")
                    .host(host)
                    .password("secret123")
                    .status(RoomStatus.WAITING)
                    .maxPlayers(4)
                    .members(new ArrayList<>())
                    .build();

            given(roomRepository.findByRoomCode("ABCDEF")).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.joinRoom("ABCDEF", player, "wrong"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("비밀번호가 일치하지 않습니다");
        }
    }

    @Nested
    @DisplayName("startGame 메서드")
    class StartGameTest {

        @Test
        @DisplayName("방장이 아닌 사용자가 시작하면 실패")
        void failsWhenNotHost() {
            // given
            Room room = Room.builder()
                    .id(UUID.randomUUID())
                    .roomCode("ABCDEF")
                    .title("테스트")
                    .host(host)
                    .status(RoomStatus.WAITING)
                    .members(new ArrayList<>())
                    .build();

            given(roomRepository.findById(room.getId())).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.startGame(room.getId(), player))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("방장만 게임을 시작할 수 있습니다");
        }
    }
}
