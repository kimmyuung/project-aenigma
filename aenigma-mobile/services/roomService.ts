import apiClient from './api';
import { API_CONFIG } from './config';

// Types
export interface HostInfo {
    id: string;
    nickname?: string;
    displayTag?: string;
    displayName: string;
}

export interface MemberInfo {
    id: string;
    nickname: string;
    displayTag: string;
    displayName: string;
    isHost: boolean;
    isReady: boolean;
    isConnected: boolean;
}

export interface RoomResponse {
    id: string;
    roomCode: string;
    title: string;
    status: 'WAITING' | 'PLAYING' | 'FINISHED' | 'CLOSED';
    currentPlayers: number;
    maxPlayers: number;
    isPrivate: boolean;
    host: HostInfo;
    members?: MemberInfo[];
    createdAt: string;
    startedAt?: string;
}

export interface CreateRoomRequest {
    title: string;
    maxPlayers?: number;
    password?: string;
}

export interface JoinRoomRequest {
    password?: string;
}

// Room Service
export const RoomService = {
    /**
     * 방 목록 조회
     */
    async getRooms(params?: {
        keyword?: string;
        status?: string;
        isPublic?: boolean;
    }): Promise<RoomResponse[]> {
        const response = await apiClient.get<RoomResponse[]>(
            API_CONFIG.ENDPOINTS.ROOMS.LIST,
            { params }
        );
        return response.data;
    },

    /**
     * 입장 가능한 방 목록
     */
    async getJoinableRooms(): Promise<RoomResponse[]> {
        const response = await apiClient.get<RoomResponse[]>(
            API_CONFIG.ENDPOINTS.ROOMS.JOINABLE
        );
        return response.data;
    },

    /**
     * 방 생성
     */
    async createRoom(request: CreateRoomRequest): Promise<RoomResponse> {
        const response = await apiClient.post<RoomResponse>(
            API_CONFIG.ENDPOINTS.ROOMS.LIST,
            request
        );
        return response.data;
    },

    /**
     * 방 상세 조회
     */
    async getRoom(roomId: string): Promise<RoomResponse> {
        const response = await apiClient.get<RoomResponse>(
            API_CONFIG.ENDPOINTS.ROOMS.BY_ID(roomId)
        );
        return response.data;
    },

    /**
     * 코드로 방 조회
     */
    async getRoomByCode(roomCode: string): Promise<RoomResponse> {
        const response = await apiClient.get<RoomResponse>(
            API_CONFIG.ENDPOINTS.ROOMS.BY_CODE(roomCode)
        );
        return response.data;
    },

    /**
     * 방 입장
     */
    async joinRoom(roomCode: string, password?: string): Promise<RoomResponse> {
        const response = await apiClient.post<RoomResponse>(
            API_CONFIG.ENDPOINTS.ROOMS.JOIN(roomCode),
            password ? { password } : {}
        );
        return response.data;
    },

    /**
     * 방 퇴장
     */
    async leaveRoom(roomId: string): Promise<void> {
        await apiClient.post(API_CONFIG.ENDPOINTS.ROOMS.LEAVE(roomId));
    },

    /**
     * 게임 시작
     */
    async startGame(roomId: string): Promise<void> {
        await apiClient.post(API_CONFIG.ENDPOINTS.ROOMS.START(roomId));
    },
};

export default RoomService;
