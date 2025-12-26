import axios from 'axios';

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

// 응답 인터셉터 - 토큰 갱신
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem('refreshToken');
                if (refreshToken) {
                    const response = await axios.post(`${API_BASE_URL}/api/auth/refresh`, null, {
                        headers: { Authorization: `Bearer ${refreshToken}` }
                    });

                    const { accessToken } = response.data;
                    localStorage.setItem('accessToken', accessToken);

                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                    return api(originalRequest);
                }
            } catch (refreshError) {
                // 리프레시 실패 시 로그아웃
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('userId');
                window.location.href = '/login';
            }
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
}

export const gameApi = {
    create: (roomId: string) =>
        api.post<Game>('/api/games', null, { params: { roomId } }),

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
};

export default api;
