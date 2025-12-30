package com.aenigma.domain.room.service;

import com.aenigma.domain.common.exception.DomainErrorCode;
import com.aenigma.domain.common.exception.DomainException;
import com.aenigma.domain.room.entity.*;
import com.aenigma.domain.room.repository.RoomMemberRepository;
import com.aenigma.domain.room.repository.RoomRepository;
import com.aenigma.domain.user.entity.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Room 도메인 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final JPAQueryFactory queryFactory;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int ROOM_CODE_LENGTH = 6;
    private static final int MAX_CODE_ATTEMPTS = 100;

    @Transactional
    public Room createRoom(User host, String title, Integer maxPlayers, String password) {
        String roomCode = generateUniqueRoomCode();

        Room room = Room.builder()
                .roomCode(roomCode)
                .title(title)
                .host(host)
                .maxPlayers(maxPlayers != null ? maxPlayers : 6)
                .password(password)
                .build();

        Room savedRoom = roomRepository.save(room);

        RoomMember hostMember = RoomMember.createHost(savedRoom, host);
        roomMemberRepository.save(hostMember);
        savedRoom.getMembers().add(hostMember);

        log.info("방 생성: {} (코드: {}, 방장: {})", title, roomCode, host.getDisplayName());
        return savedRoom;
    }

    @Transactional
    public RoomMember joinRoom(String roomCode, User user, String password) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new DomainException(DomainErrorCode.ROOM_NOT_FOUND));

        if (!room.canJoin()) {
            throw new DomainException(DomainErrorCode.ROOM_NOT_JOINABLE);
        }

        if (room.isPrivate() && !room.getPassword().equals(password)) {
            throw new DomainException(DomainErrorCode.WRONG_PASSWORD);
        }

        if (roomMemberRepository.existsByRoomAndUser(room, user)) {
            throw new DomainException(DomainErrorCode.ALREADY_IN_ROOM);
        }

        RoomMember member = RoomMember.createMember(room, user);
        RoomMember savedMember = roomMemberRepository.save(member);
        room.getMembers().add(savedMember);

        log.info("방 입장: {} -> {} ({})", user.getDisplayName(), room.getTitle(), roomCode);
        return savedMember;
    }

    @Transactional
    public void leaveRoom(UUID roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DomainException(DomainErrorCode.ROOM_NOT_FOUND));

        RoomMember member = roomMemberRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new DomainException(DomainErrorCode.NOT_IN_ROOM));

        boolean wasHost = member.getIsHost();
        roomMemberRepository.delete(member);
        room.getMembers().remove(member);

        if (wasHost) {
            if (room.getMembers().isEmpty()) {
                room.close();
            } else {
                RoomMember newHost = room.getMembers().get(0);
                newHost.promoteToHost();
                room.changeHost(newHost.getUser());
            }
        }

        log.info("방 퇴장: {} <- {}", user.getDisplayName(), room.getTitle());
    }

    @Transactional
    public void startGame(UUID roomId, User host) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DomainException(DomainErrorCode.ROOM_NOT_FOUND));

        if (!room.getHost().getId().equals(host.getId())) {
            throw new DomainException(DomainErrorCode.NOT_ROOM_HOST);
        }

        long readyCount = roomMemberRepository.countByRoomAndIsReadyTrue(room);
        if (readyCount < room.getMembers().size()) {
            throw new DomainException(DomainErrorCode.NOT_ALL_READY);
        }

        room.startGame();
        log.info("게임 시작: {} (참여자: {}명)", room.getTitle(), room.getCurrentPlayerCount());
    }

    public List<Room> searchRooms(String keyword, RoomStatus status, Boolean isPublic) {
        QRoom room = QRoom.room;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(room.status.ne(RoomStatus.CLOSED));

        if (status != null) {
            builder.and(room.status.eq(status));
        }

        if (keyword != null && !keyword.isBlank()) {
            builder.and(room.title.containsIgnoreCase(keyword));
        }

        if (isPublic != null) {
            if (isPublic) {
                builder.and(room.password.isNull());
            } else {
                builder.and(room.password.isNotNull());
            }
        }

        return queryFactory
                .selectFrom(room)
                .where(builder)
                .orderBy(room.createdAt.desc())
                .limit(50)
                .fetch();
    }

    public List<Room> findJoinableRooms() {
        QRoom room = QRoom.room;

        return queryFactory
                .selectFrom(room)
                .where(
                        room.status.eq(RoomStatus.WAITING),
                        room.members.size().lt(room.maxPlayers))
                .orderBy(room.createdAt.desc())
                .limit(20)
                .fetch();
    }

    public Optional<Room> findById(UUID id) {
        return roomRepository.findById(id);
    }

    public Optional<Room> findByRoomCode(String roomCode) {
        return roomRepository.findByRoomCode(roomCode);
    }

    private String generateUniqueRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String code;
        int attempts = 0;

        do {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
            code = sb.toString();
            attempts++;

            if (attempts > MAX_CODE_ATTEMPTS) {
                throw new DomainException(DomainErrorCode.ROOM_CODE_GENERATION_FAILED);
            }
        } while (roomRepository.existsByRoomCode(code));

        return code;
    }
}
