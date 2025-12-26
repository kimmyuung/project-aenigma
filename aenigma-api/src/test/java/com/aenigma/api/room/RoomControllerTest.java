package com.aenigma.api.room;

import com.aenigma.api.room.controller.RoomController;
import com.aenigma.api.room.dto.CreateRoomRequest;
import com.aenigma.api.room.dto.JoinRoomRequest;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.entity.RoomStatus;
import com.aenigma.domain.room.service.RoomService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("RoomController 테스트")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private UserService userService;

    private User testUser;
    private Room testRoom;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .username("test_user")
                .nickname("테스터")
                .displayTag("1234")
                .role(UserRole.GUEST)
                .build();

        testRoom = Room.builder()
                .id(UUID.randomUUID())
                .title("테스트 방")
                .roomCode("ABC123")
                .host(testUser)
                .maxPlayers(6)
                .status(RoomStatus.WAITING)
                .build();
    }

    @Nested
    @DisplayName("방 생성 API")
    class CreateRoom {

        @Test
        @WithMockUser
        @DisplayName("성공적으로 방을 생성한다")
        void success() throws Exception {
            // given
            CreateRoomRequest request = new CreateRoomRequest();
            setField(request, "title", "테스트 방");
            setField(request, "maxPlayers", 6);

            given(userService.findById(userId)).willReturn(Optional.of(testUser));
            given(roomService.createRoom(any(User.class), eq("테스트 방"), eq(6), isNull()))
                    .willReturn(testRoom);

            // when & then
            mockMvc.perform(post("/api/rooms")
                    .with(csrf())
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("테스트 방"))
                    .andExpect(jsonPath("$.roomCode").value("ABC123"))
                    .andExpect(jsonPath("$.maxPlayers").value(6));
        }

        @Test
        @WithMockUser
        @DisplayName("제목이 비어있으면 실패한다")
        void failWithEmptyTitle() throws Exception {
            // given
            CreateRoomRequest request = new CreateRoomRequest();
            setField(request, "title", "");
            setField(request, "maxPlayers", 6);

            given(userService.findById(userId)).willReturn(Optional.of(testUser));

            // when & then
            mockMvc.perform(post("/api/rooms")
                    .with(csrf())
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("방 입장 API")
    class JoinRoom {

        @Test
        @WithMockUser
        @DisplayName("성공적으로 방에 입장한다")
        void success() throws Exception {
            // given
            JoinRoomRequest request = new JoinRoomRequest();
            setField(request, "roomCode", "ABC123");

            RoomMember member = RoomMember.builder()
                    .id(UUID.randomUUID())
                    .room(testRoom)
                    .user(testUser)
                    .build();

            given(userService.findById(userId)).willReturn(Optional.of(testUser));
            given(roomService.joinRoom(eq("ABC123"), any(User.class), isNull()))
                    .willReturn(member);

            // when & then
            mockMvc.perform(post("/api/rooms/join")
                    .with(csrf())
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomCode").value("ABC123"));
        }
    }

    @Nested
    @DisplayName("방 퇴장 API")
    class LeaveRoom {

        @Test
        @WithMockUser
        @DisplayName("성공적으로 방에서 퇴장한다")
        void success() throws Exception {
            // given
            UUID roomId = testRoom.getId();

            given(userService.findById(userId)).willReturn(Optional.of(testUser));
            doNothing().when(roomService).leaveRoom(eq(roomId), any(User.class));

            // when & then
            mockMvc.perform(post("/api/rooms/{roomId}/leave", roomId)
                    .with(csrf())
                    .header("X-User-Id", userId.toString()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("방 조회 API")
    class GetRoom {

        @Test
        @WithMockUser
        @DisplayName("방 상세 정보를 조회한다")
        void getById() throws Exception {
            // given
            UUID roomId = testRoom.getId();
            given(roomService.findById(roomId)).willReturn(Optional.of(testRoom));

            // when & then
            mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(roomId.toString()))
                    .andExpect(jsonPath("$.title").value("테스트 방"));
        }

        @Test
        @WithMockUser
        @DisplayName("방 코드로 조회한다")
        void getByCode() throws Exception {
            // given
            given(roomService.findByRoomCode("ABC123")).willReturn(Optional.of(testRoom));

            // when & then
            mockMvc.perform(get("/api/rooms/code/{roomCode}", "ABC123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomCode").value("ABC123"));
        }

        @Test
        @WithMockUser
        @DisplayName("방 목록을 조회한다")
        void getRooms() throws Exception {
            // given
            given(roomService.searchRooms(isNull(), isNull(), isNull()))
                    .willReturn(List.of(testRoom));

            // when & then
            mockMvc.perform(get("/api/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser
        @DisplayName("입장 가능한 방 목록을 조회한다")
        void getJoinableRooms() throws Exception {
            // given
            given(roomService.findJoinableRooms()).willReturn(List.of(testRoom));

            // when & then
            mockMvc.perform(get("/api/rooms/joinable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
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
