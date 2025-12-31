import axios, { type AxiosError } from 'axios';
import { type ApiErrorResponse, getErrorMessage, getStatusMessage, formatValidationErrors } from './errorMessages';

// Axios 타입 확장 (_retry 속성 추가)
declare module 'axios' {
    interface InternalAxiosRequestConfig {
        _retry?: boolean;
    }
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Axios 인스턴스 생성
export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 요청 인터셉터 - 토큰 자동 추가
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        const userId = localStorage.getItem('userId');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        if (userId) {
            config.headers['X-User-Id'] = userId;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터 - 에러 처리 및 토큰 갱신
api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ApiErrorResponse>) => {
        const { response, config: originalRequest } = error;

        // 401 처리 - 토큰 갱신 시도
        if (response?.status === 401 && originalRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem('refreshToken');
                if (refreshToken) {
                    const refreshResponse = await axios.post(`${API_BASE_URL}/api/auth/refresh`, null, {
                        headers: { Authorization: `Bearer ${refreshToken}` }
                    });

                    const { accessToken } = refreshResponse.data;
                    localStorage.setItem('accessToken', accessToken);

                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                    return api(originalRequest);
                }
            } catch (refreshError) {
                // 리프레시 실패 시 로그아웃
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('userId');

                // 토스트 이벤트 발생
                window.dispatchEvent(new CustomEvent('api-error', {
                    detail: {
                        code: 'A004',
                        message: '세션이 만료되었습니다. 다시 로그인해주세요.',
                        status: 401,
                        shouldRedirect: true,
                    }
                }));

                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        // 에러 응답이 있는 경우 사용자 친화적 메시지로 변환
        if (response?.data) {
            const { code, message, errors } = response.data;
            const status = response.status;

            let userMessage: string;

            // Validation 에러인 경우 필드별 메시지 표시
            if (code === 'C002' && errors && errors.length > 0) {
                userMessage = formatValidationErrors(errors);
            } else {
                // 에러 코드 매핑 또는 백엔드 메시지 사용
                userMessage = getErrorMessage(code, message);
            }

            // 에러 이벤트 발생 (ToastProvider에서 처리)
            window.dispatchEvent(new CustomEvent('api-error', {
                detail: {
                    code,
                    message: userMessage,
                    status,
                    shouldRedirect: false,
                }
            }));
        } else if (response?.status) {
            // 응답 데이터가 없는 경우 상태 코드 기반 메시지
            const userMessage = getStatusMessage(response.status);

            window.dispatchEvent(new CustomEvent('api-error', {
                detail: {
                    code: 'UNKNOWN',
                    message: userMessage,
                    status: response.status,
                    shouldRedirect: false,
                }
            }));
        } else if (!response) {
            // 네트워크 에러 (응답 없음)
            window.dispatchEvent(new CustomEvent('api-error', {
                detail: {
                    code: 'NETWORK',
                    message: '네트워크 연결을 확인해주세요.',
                    status: 0,
                    shouldRedirect: false,
                }
            }));
        }

        return Promise.reject(error);
    }
);

// === Auth API ===
export interface RegisterRequest {
    nickname: string;
}

export interface LoginRequest {
    username: string;
}

export interface AuthResponse {
    userId: string;
    username: string;
    nickname: string;
    displayTag: string;
    displayName: string;
    accessToken: string;
    refreshToken: string;
}

export const authApi = {
    register: (data: RegisterRequest) =>
        api.post<AuthResponse>('/api/auth/register', data),

    login: (data: LoginRequest) =>
        api.post<AuthResponse>('/api/auth/login', data),

    refresh: (refreshToken: string) =>
        api.post<AuthResponse>('/api/auth/refresh', null, {
            headers: { Authorization: `Bearer ${refreshToken}` }
        }),
};

// === Room API ===
export interface Room {
    id: string;
    roomCode: string;
    title: string;
    status: 'WAITING' | 'PLAYING' | 'FINISHED' | 'CLOSED';
    maxPlayers: number;
    currentPlayerCount: number;
    hostNickname: string;
    isPrivate: boolean;
    createdAt: string;
}

export interface CreateRoomRequest {
    title: string;
    maxPlayers: number;
    password?: string;
}

export interface JoinRoomRequest {
    roomCode: string;
    password?: string;
}

export const roomApi = {
    list: () =>
        api.get<Room[]>('/api/rooms'),

    joinable: () =>
        api.get<Room[]>('/api/rooms/joinable'),

    get: (roomId: string) =>
        api.get<Room>(`/api/rooms/${roomId}`),

    getById: (roomId: string) =>
        api.get<Room>(`/api/rooms/${roomId}`),

    getByCode: (roomCode: string) =>
        api.get<Room>(`/api/rooms/code/${roomCode}`),

    create: (data: CreateRoomRequest) =>
        api.post<Room>('/api/rooms', data),

    join: (data: JoinRoomRequest) =>
        api.post<Room>('/api/rooms/join', data),

    leave: (roomId: string) =>
        api.post(`/api/rooms/${roomId}/leave`),

    start: (roomId: string) =>
        api.post(`/api/rooms/${roomId}/start`),
};

// === Game API ===
export interface GamePlayer {
    id: string;
    playerId: string;
    userId: string;
    nickname: string;
    displayTag: string;
    role?: string;
    isAlive: boolean;
}

export interface Game {
    id: string;
    roomId: string;
    phase: 'INTRO' | 'LOBBY' | 'INVESTIGATION' | 'FINAL_VOTE' | 'CONCLUSION' | 'FINISHED';
    investigationRound: number;
    maxInvestigationRounds: number;
    players: GamePlayer[];
    myRole?: string;
    startedAt?: string;
    finishedAt?: string;
    // 게임 결과 관련 (게임 종료 시에만 포함)
    winnerTeam?: 'CRIMINAL' | 'SUSPECT' | 'DETECTIVE' | null;
    scenarioTitle?: string;
    scenarioSummary?: string;
}

export interface Clue {
    id: string;
    title: string;
    content: string;
    clueType: 'PUBLIC' | 'PERSONAL' | 'HIDDEN';
    revealRound?: number;
    importance: number;
    imageUrl?: string;
    isDiscovered: boolean;
    discoveredByNickname?: string;
}

export interface AlibiEntry {
    time: string;
    location: string;
    activity: string;
    witnesses?: string[];
}

export interface RoleDetail {
    playerId: string;
    nickname: string;
    displayTag: string;
    roleType: string;
    roleName?: string;
    description?: string;
    secretInfo?: string;
    objective?: string;
    alibi?: string; // JSON string of AlibiEntry[]
    isAlive: boolean;
}

export const gameApi = {
    create: (roomId: string, scenarioId?: string) =>
        api.post<Game>('/api/games', null, {
            params: { roomId, scenarioId }
        }),

    get: (gameId: string) =>
        api.get<Game>(`/api/games/${gameId}`),

    getById: (gameId: string) =>
        api.get<Game>(`/api/games/${gameId}`),

    getActive: (roomId: string) =>
        api.get<Game>(`/api/games/room/${roomId}/active`),

    start: (gameId: string) =>
        api.post<Game>(`/api/games/${gameId}/start`),

    nextPhase: (gameId: string) =>
        api.post<Game>(`/api/games/${gameId}/next-phase`),

    getStats: () =>
        api.get<Record<string, unknown>>('/api/games/stats'),

    // 새로 추가된 API
    getClues: (gameId: string) =>
        api.get<Clue[]>(`/api/games/${gameId}/clues`),

    getMyRole: (gameId: string) =>
        api.get<RoleDetail>(`/api/games/${gameId}/my-role`),

    vote: (gameId: string, targetPlayerId: string) =>
        api.post<{ success: boolean; message: string }>(`/api/games/${gameId}/vote`, {
            targetPlayerId
        }),
};

// === Vote API ===
export interface VoteResultData {
    gameId: string;
    round: number;
    results: Record<string, number>; // playerId -> voteCount
    isComplete: boolean;
    totalVotes: number;
    expectedVotes: number;
}

export interface VotePlayerInfo {
    playerId: string;
    userId: string;
    nickname: string;
    isAlive: boolean;
}

export const voteApi = {
    /** 투표 결과 조회 */
    getResults: (gameId: string, round: number) =>
        api.get<VoteResultData>(`/api/games/${gameId}/votes/results`, { params: { round } }),

    /** 투표 상태 조회 */
    getStatus: (gameId: string, round: number) =>
        api.get<{ gameId: string; round: number; isComplete: boolean }>(`/api/games/${gameId}/votes/status`, { params: { round } }),

    /** 최다 득표자 조회 */
    getMostVoted: (gameId: string, round: number) =>
        api.get<VotePlayerInfo[]>(`/api/games/${gameId}/votes/top`, { params: { round } }),

    /** 내 투표 기록 조회 */
    getMyVotes: (gameId: string) =>
        api.get<Array<{ id: string; round: number; targetNickname: string }>>(`/api/games/${gameId}/votes/my`),
};

export default api;


