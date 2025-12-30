package com.aenigma.domain.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도메인 에러 코드
 * 
 * Domain 모듈은 spring-web에 의존하지 않으므로 HTTP 상태코드를 int로 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum DomainErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(500, "C001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(400, "C002", "잘못된 입력 값입니다."),
    RESOURCE_NOT_FOUND(404, "C004", "요청한 리소스를 찾을 수 없습니다."),

    // User
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다."),
    USERNAME_GENERATION_FAILED(500, "U004", "고유 username 생성 실패: 최대 시도 횟수 초과"),
    DISPLAYTAG_GENERATION_FAILED(500, "U005", "고유 displayTag 생성 실패: 최대 시도 횟수 초과"),

    // Room
    ROOM_NOT_FOUND(404, "R001", "방을 찾을 수 없습니다."),
    ROOM_FULL(400, "R002", "방이 가득 찼습니다."),
    ROOM_NOT_JOINABLE(400, "R003", "입장할 수 없는 방입니다."),
    WRONG_PASSWORD(401, "R004", "비밀번호가 일치하지 않습니다."),
    ALREADY_IN_ROOM(409, "R005", "이미 방에 참여 중입니다."),
    NOT_ROOM_HOST(403, "R006", "방장만 수행할 수 있는 작업입니다."),
    NOT_ALL_READY(400, "R007", "모든 참여자가 준비되지 않았습니다."),
    NOT_IN_ROOM(400, "R008", "참여 중인 방이 아닙니다."),
    ROOM_CODE_GENERATION_FAILED(500, "R009", "방 코드 생성 실패: 최대 시도 횟수 초과"),
    ROOM_NOT_WAITING(400, "R010", "대기 상태에서만 게임을 시작할 수 있습니다."),
    ROOM_NOT_IN_GAME(400, "R011", "진행 중인 게임만 종료할 수 있습니다."),

    // Game
    GAME_NOT_FOUND(404, "G001", "게임을 찾을 수 없습니다."),
    GAME_ALREADY_STARTED(400, "G002", "이미 시작된 게임입니다."),
    GAME_ALREADY_FINISHED(400, "G003", "이미 종료된 게임입니다."),
    INVALID_GAME_PHASE(400, "G004", "현재 게임 단계에서는 수행할 수 없습니다."),
    PLAYER_NOT_FOUND(404, "G005", "플레이어를 찾을 수 없습니다."),
    PLAYER_ALREADY_DEAD(400, "G006", "이미 사망한 플레이어입니다."),
    SCENARIO_NOT_SET(400, "G007", "시나리오가 없는 게임입니다."),
    INVALID_INVESTIGATION_ROUND(400, "G008", "조사 라운드는 1~3 사이여야 합니다."),
    PLAYER_COUNT_MISMATCH(400, "G009", "플레이어 수가 역할 수와 맞지 않습니다."),

    // Vote
    ALREADY_VOTED(409, "V001", "이미 투표하셨습니다."),
    CANNOT_VOTE_DEAD_PLAYER(400, "V002", "사망한 플레이어는 투표할 수 없습니다."),
    CANNOT_VOTE_FOR_DEAD(400, "V003", "사망한 플레이어에게 투표할 수 없습니다."),

    // Chat
    MESSAGE_EMPTY(400, "CH001", "메시지 내용이 비어있습니다."),
    MESSAGE_TOO_LONG(400, "CH002", "메시지가 너무 깁니다."),
    CANNOT_WHISPER_SELF(400, "CH003", "자신에게 귓속말을 보낼 수 없습니다."),
    PLAYER_NOT_IN_GAME(403, "CH004", "게임에 참여하지 않은 플레이어입니다."),
    DEAD_PLAYER_CANNOT_CHAT(403, "CH005", "사망한 플레이어는 메시지를 보낼 수 없습니다."),

    // Scenario
    SCENARIO_NOT_FOUND(404, "S001", "시나리오를 찾을 수 없습니다."),
    ALREADY_PURCHASED(409, "S002", "이미 구매한 시나리오입니다."),
    OWN_SCENARIO(400, "S003", "본인이 작성한 시나리오입니다."),
    ALREADY_REVIEWED(409, "S004", "이미 리뷰를 작성했습니다."),
    PURCHASE_REQUIRED(403, "S005", "구매한 시나리오만 리뷰할 수 있습니다."),
    CANNOT_REVIEW_OWN(400, "S006", "본인 시나리오는 리뷰할 수 없습니다."),
    NOT_SCENARIO_AUTHOR(403, "S007", "본인의 시나리오만 공개할 수 있습니다."),
    SCENARIO_HAS_NO_ROLES(400, "S008", "역할이 없는 시나리오는 공개할 수 없습니다."),
    ALREADY_REFUNDED(409, "S009", "이미 환불된 구매입니다."),
    INVALID_RATING(400, "S010", "평점은 1~5 사이여야 합니다.");

    private final int status;
    private final String code;
    private final String message;
}
