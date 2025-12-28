import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';

// Axios 인스턴스 생성
export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

// 토큰 저장/조회
export const tokenStorage = {
    async getAccessToken(): Promise<string | null> {
        return await SecureStore.getItemAsync('accessToken');
    },
    async setAccessToken(token: string): Promise<void> {
        await SecureStore.setItemAsync('accessToken', token);
    },
    async getRefreshToken(): Promise<string | null> {
        return await SecureStore.getItemAsync('refreshToken');
    },
    async setRefreshToken(token: string): Promise<void> {
        await SecureStore.setItemAsync('refreshToken', token);
    },
    async getUserId(): Promise<string | null> {
        return await SecureStore.getItemAsync('userId');
    },
    async setUserId(id: string): Promise<void> {
        await SecureStore.setItemAsync('userId', id);
    },
    async clear(): Promise<void> {
        await SecureStore.deleteItemAsync('accessToken');
        await SecureStore.deleteItemAsync('refreshToken');
        await SecureStore.deleteItemAsync('userId');
    },
};

// 요청 인터셉터 - 토큰 자동 추가
api.interceptors.request.use(
    async (config) => {
        const token = await tokenStorage.getAccessToken();
        const userId = await tokenStorage.getUserId();

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
    list: () => api.get<Room[]>('/api/rooms'),
    joinable: () => api.get<Room[]>('/api/rooms/joinable'),
    get: (roomId: string) => api.get<Room>(`/api/rooms/${roomId}`),
    getByCode: (roomCode: string) => api.get<Room>(`/api/rooms/code/${roomCode}`),
    create: (data: CreateRoomRequest) => api.post<Room>('/api/rooms', data),
    join: (data: JoinRoomRequest) => api.post<Room>('/api/rooms/join', data),
    leave: (roomId: string) => api.post(`/api/rooms/${roomId}/leave`),
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
}

export interface Clue {
    id: string;
    title: string;
    content: string;
    clueType: 'PUBLIC' | 'PERSONAL' | 'HIDDEN';
    isDiscovered: boolean;
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
    alibi?: string;
    isAlive: boolean;
}

export const gameApi = {
    get: (gameId: string) => api.get<Game>(`/api/games/${gameId}`),
    getActive: (roomId: string) => api.get<Game>(`/api/games/room/${roomId}/active`),
    getClues: (gameId: string) => api.get<Clue[]>(`/api/games/${gameId}/clues`),
    getMyRole: (gameId: string) => api.get<RoleDetail>(`/api/games/${gameId}/my-role`),
    vote: (gameId: string, targetPlayerId: string) =>
        api.post<{ success: boolean }>(`/api/games/${gameId}/vote`, { targetPlayerId }),
};

export default api;
