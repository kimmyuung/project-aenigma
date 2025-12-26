package com.aenigma.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U002", "이미 사용 중인 사용자명입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "U003", "유효하지 않은 닉네임입니다."),

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "방을 찾을 수 없습니다."),
    ROOM_FULL(HttpStatus.BAD_REQUEST, "R002", "방이 가득 찼습니다."),
    ROOM_NOT_JOINABLE(HttpStatus.BAD_REQUEST, "R003", "입장할 수 없는 방입니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "R004", "비밀번호가 일치하지 않습니다."),
    ALREADY_IN_ROOM(HttpStatus.CONFLICT, "R005", "이미 방에 참여 중입니다."),
    NOT_ROOM_HOST(HttpStatus.FORBIDDEN, "R006", "방장만 수행할 수 있는 작업입니다."),
    NOT_ALL_READY(HttpStatus.BAD_REQUEST, "R007", "모든 참여자가 준비되지 않았습니다."),

    // Game
    GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "G001", "게임을 찾을 수 없습니다."),
    GAME_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "G002", "이미 시작된 게임입니다."),
    GAME_ALREADY_FINISHED(HttpStatus.BAD_REQUEST, "G003", "이미 종료된 게임입니다."),
    INVALID_GAME_PHASE(HttpStatus.BAD_REQUEST, "G004", "현재 게임 단계에서는 수행할 수 없습니다."),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "G005", "플레이어를 찾을 수 없습니다."),
    PLAYER_ALREADY_DEAD(HttpStatus.BAD_REQUEST, "G006", "이미 사망한 플레이어입니다."),

    // Vote
    ALREADY_VOTED(HttpStatus.CONFLICT, "V001", "이미 투표하셨습니다."),
    CANNOT_VOTE_DEAD_PLAYER(HttpStatus.BAD_REQUEST, "V002", "사망한 플레이어는 투표할 수 없습니다."),
    CANNOT_VOTE_FOR_DEAD(HttpStatus.BAD_REQUEST, "V003", "사망한 플레이어에게 투표할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
