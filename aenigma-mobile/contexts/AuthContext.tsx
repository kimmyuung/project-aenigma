import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi, tokenStorage, AuthResponse } from '../services/api';

interface User {
    userId: string;
    username: string;
    nickname: string;
    displayTag: string;
}

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    isAuthenticated: boolean;
    login: (username: string) => Promise<void>;
    register: (nickname: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        checkAuth();
    }, []);

    const checkAuth = async () => {
        try {
            const token = await tokenStorage.getAccessToken();
            const userId = await tokenStorage.getUserId();

            if (token && userId) {
                // TODO: 서버에서 유저 정보 가져오기
                setUser({
                    userId,
                    username: '',
                    nickname: '',
                    displayTag: '',
                });
            }
        } catch (error) {
            console.error('Auth check failed', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleAuthResponse = async (response: AuthResponse) => {
        await tokenStorage.setAccessToken(response.accessToken);
        await tokenStorage.setRefreshToken(response.refreshToken);
        await tokenStorage.setUserId(response.userId);

        setUser({
            userId: response.userId,
            username: response.username,
            nickname: response.nickname,
            displayTag: response.displayTag,
        });
    };

    const login = async (username: string) => {
        setIsLoading(true);
        try {
            const response = await authApi.login({ username });
            await handleAuthResponse(response.data);
        } finally {
            setIsLoading(false);
        }
    };

    const register = async (nickname: string) => {
        setIsLoading(true);
        try {
            const response = await authApi.register({ nickname });
            await handleAuthResponse(response.data);
        } finally {
            setIsLoading(false);
        }
    };

    const logout = async () => {
        await tokenStorage.clear();
        setUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoading,
                isAuthenticated: !!user,
                login,
                register,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
