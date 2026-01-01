/**
 * AENIGMA 표준 에러 코드 시스템
 * 
 * 백엔드 에러 코드와 프론트엔드 에러 처리를 통합합니다.
 */

// ========================================
// 에러 코드 카테고리별 상수
// ========================================

/** 공통 에러 코드 */
export const CommonErrorCode = {
    INTERNAL_ERROR: 'C001',
    VALIDATION_FAILED: 'C002',
    UNSUPPORTED_METHOD: 'C003',
    NOT_FOUND: 'C004',
} as const;
export type CommonErrorCode = typeof CommonErrorCode[keyof typeof CommonErrorCode];

/** 인증 에러 코드 */
export const AuthErrorCode = {
    UNAUTHORIZED: 'A001',
    ACCESS_DENIED: 'A002',
    TOKEN_EXPIRED: 'A003',
    REFRESH_TOKEN_EXPIRED: 'A004',
} as const;
export type AuthErrorCode = typeof AuthErrorCode[keyof typeof AuthErrorCode];

/** 사용자 에러 코드 */
export const UserErrorCode = {
    USER_NOT_FOUND: 'U001',
    NICKNAME_DUPLICATED: 'U002',
    INVALID_NICKNAME: 'U003',
    REGISTRATION_FAILED: 'U004',
    ACCOUNT_CREATION_FAILED: 'U005',
} as const;
export type UserErrorCode = typeof UserErrorCode[keyof typeof UserErrorCode];

/** 방 에러 코드 */
export const RoomErrorCode = {
    ROOM_NOT_FOUND: 'R001',
    ROOM_FULL: 'R002',
    ROOM_NOT_JOINABLE: 'R003',
    WRONG_PASSWORD: 'R004',
    ALREADY_MEMBER: 'R005',
    HOST_ONLY: 'R006',
    NOT_ALL_READY: 'R007',
    NOT_MEMBER: 'R008',
    CREATION_FAILED: 'R009',
    INVALID_STATE_FOR_START: 'R010',
    INVALID_STATE_FOR_END: 'R011',
} as const;
export type RoomErrorCode = typeof RoomErrorCode[keyof typeof RoomErrorCode];

/** 게임 에러 코드 */
export const GameErrorCode = {
    GAME_NOT_FOUND: 'G001',
    GAME_ALREADY_STARTED: 'G002',
    GAME_ALREADY_FINISHED: 'G003',
    INVALID_PHASE: 'G004',
    PLAYER_NOT_FOUND: 'G005',
    PLAYER_ALREADY_ELIMINATED: 'G006',
    NO_SCENARIO: 'G007',
    INVALID_ROUND_SETTINGS: 'G008',
    PLAYER_COUNT_MISMATCH: 'G009',
} as const;
export type GameErrorCode = typeof GameErrorCode[keyof typeof GameErrorCode];

/** 투표 에러 코드 */
export const VoteErrorCode = {
    ALREADY_VOTED: 'V001',
    VOTER_ELIMINATED: 'V002',
    TARGET_ELIMINATED: 'V003',
} as const;
export type VoteErrorCode = typeof VoteErrorCode[keyof typeof VoteErrorCode];

/** 채팅 에러 코드 */
export const ChatErrorCode = {
    EMPTY_MESSAGE: 'CH001',
    MESSAGE_TOO_LONG: 'CH002',
    SELF_WHISPER: 'CH003',
    NOT_IN_GAME: 'CH004',
    SENDER_ELIMINATED: 'CH005',
} as const;
export type ChatErrorCode = typeof ChatErrorCode[keyof typeof ChatErrorCode];

/** 시나리오 에러 코드 */
export const ScenarioErrorCode = {
    SCENARIO_NOT_FOUND: 'S001',
    ALREADY_PURCHASED: 'S002',
    OWN_SCENARIO: 'S003',
    ALREADY_REVIEWED: 'S004',
    PURCHASE_REQUIRED: 'S005',
    CANNOT_REVIEW_OWN: 'S006',
    ONLY_AUTHOR_CAN_PUBLISH: 'S007',
    NO_ROLES_TO_PUBLISH: 'S008',
    ALREADY_REFUNDED: 'S009',
    INVALID_RATING: 'S010',
} as const;
export type ScenarioErrorCode = typeof ScenarioErrorCode[keyof typeof ScenarioErrorCode];

// ========================================
// 에러 심각도
// ========================================

export type ErrorSeverity = 'info' | 'warning' | 'error' | 'critical';

/** 에러 코드별 심각도 매핑 */
export const ERROR_SEVERITY: Record<string, ErrorSeverity> = {
    // Critical - 사용자 세션에 영향
    [AuthErrorCode.TOKEN_EXPIRED]: 'critical',
    [AuthErrorCode.REFRESH_TOKEN_EXPIRED]: 'critical',
    [CommonErrorCode.INTERNAL_ERROR]: 'critical',

    // Error - 작업 실패
    [RoomErrorCode.ROOM_NOT_FOUND]: 'error',
    [GameErrorCode.GAME_NOT_FOUND]: 'error',
    [UserErrorCode.USER_NOT_FOUND]: 'error',
    [VoteErrorCode.ALREADY_VOTED]: 'error',

    // Warning - 주의 필요
    [RoomErrorCode.ROOM_FULL]: 'warning',
    [RoomErrorCode.WRONG_PASSWORD]: 'warning',
    [CommonErrorCode.VALIDATION_FAILED]: 'warning',
    [ChatErrorCode.MESSAGE_TOO_LONG]: 'warning',

    // Info - 정보성
    [RoomErrorCode.ALREADY_MEMBER]: 'info',
    [ScenarioErrorCode.ALREADY_PURCHASED]: 'info',
};

// ========================================
// 에러 아이콘
// ========================================

export const ERROR_ICONS: Record<ErrorSeverity, string> = {
    info: 'ℹ️',
    warning: '⚠️',
    error: '❌',
    critical: '🚨',
};

// ========================================
// 헬퍼 함수
// ========================================

/**
 * 에러 코드에서 심각도 추출
 */
export function getErrorSeverity(code: string): ErrorSeverity {
    return ERROR_SEVERITY[code] || 'error';
}

/**
 * 에러 코드에서 아이콘 추출
 */
export function getErrorIcon(code: string): string {
    const severity = getErrorSeverity(code);
    return ERROR_ICONS[severity];
}

/**
 * 에러 코드가 인증 관련인지 확인
 */
export function isAuthError(code: string): boolean {
    return code.startsWith('A');
}

/**
 * 에러 코드가 리다이렉트를 필요로 하는지 확인
 */
export function shouldRedirectToLogin(code: string): boolean {
    return code === AuthErrorCode.TOKEN_EXPIRED ||
        code === AuthErrorCode.REFRESH_TOKEN_EXPIRED ||
        code === AuthErrorCode.UNAUTHORIZED;
}

/**
 * 모든 에러 코드 타입 통합
 */
export type ErrorCode =
    | CommonErrorCode
    | AuthErrorCode
    | UserErrorCode
    | RoomErrorCode
    | GameErrorCode
    | VoteErrorCode
    | ChatErrorCode
    | ScenarioErrorCode;
