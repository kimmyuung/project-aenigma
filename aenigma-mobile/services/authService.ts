import apiClient, { TokenManager } from './api';
import { API_CONFIG } from './config';

// Types
export interface UserInfo {
    id: string;
    username: string;
    nickname: string;
    displayTag: string;
    displayName: string;
    role: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    user: UserInfo;
}

export interface GuestLoginRequest {
    nickname: string;
}

// Auth Service
export const AuthService = {
    /**
     * 게스트 로그인
     */
    async guestLogin(nickname: string): Promise<LoginResponse> {
        const response = await apiClient.post<LoginResponse>(
            API_CONFIG.ENDPOINTS.AUTH.GUEST_LOGIN,
            { nickname }
        );

        // Store tokens
        await TokenManager.setTokens(
            response.data.accessToken,
            response.data.refreshToken
        );

        return response.data;
    },

    /**
     * 기존 사용자 로그인
     */
    async login(username: string): Promise<LoginResponse> {
        const response = await apiClient.post<LoginResponse>(
            API_CONFIG.ENDPOINTS.AUTH.LOGIN,
            null,
            { params: { username } }
        );

        await TokenManager.setTokens(
            response.data.accessToken,
            response.data.refreshToken
        );

        return response.data;
    },

    /**
     * 로그아웃
     */
    async logout(): Promise<void> {
        await TokenManager.clearTokens();
    },

    /**
     * 현재 로그인 상태 확인
     */
    async isLoggedIn(): Promise<boolean> {
        const token = await TokenManager.getAccessToken();
        return !!token;
    },
};

export default AuthService;
