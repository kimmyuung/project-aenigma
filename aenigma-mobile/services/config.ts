// API Configuration
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';

export const API_CONFIG = {
    BASE_URL: API_BASE_URL,
    ENDPOINTS: {
        AUTH: {
            GUEST_LOGIN: '/api/v1/auth/guest',
            LOGIN: '/api/v1/auth/login',
            REFRESH: '/api/v1/auth/refresh',
        },
        ROOMS: {
            LIST: '/api/v1/rooms',
            JOINABLE: '/api/v1/rooms/joinable',
            BY_ID: (id: string) => `/api/v1/rooms/${id}`,
            BY_CODE: (code: string) => `/api/v1/rooms/code/${code}`,
            JOIN: (code: string) => `/api/v1/rooms/code/${code}/join`,
            LEAVE: (id: string) => `/api/v1/rooms/${id}/leave`,
            START: (id: string) => `/api/v1/rooms/${id}/start`,
        },
    },
    TIMEOUT: 10000,
};

export default API_CONFIG;
