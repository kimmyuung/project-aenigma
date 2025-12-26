import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi, type AuthResponse } from '../api/client';

interface User {
    userId: string;
    username: string;
    nickname: string;
    displayTag: string;
    displayName: string;
}

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    isAuthenticated: boolean;
    login: (username: string) => Promise<void>;
    register: (nickname: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // 저장된 사용자 정보 복원
        const savedUser = localStorage.getItem('user');
        const accessToken = localStorage.getItem('accessToken');

        if (savedUser && accessToken) {
            setUser(JSON.parse(savedUser));
        }
        setIsLoading(false);
    }, []);

    const handleAuthResponse = (response: AuthResponse) => {
        const userData: User = {
            userId: response.userId,
            username: response.username,
            nickname: response.nickname,
            displayTag: response.displayTag,
            displayName: response.displayName,
        };

        setUser(userData);
        localStorage.setItem('user', JSON.stringify(userData));
        localStorage.setItem('userId', response.userId);
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
    };

    const register = async (nickname: string) => {
        const response = await authApi.register({ nickname });
        handleAuthResponse(response.data);
    };

    const login = async (username: string) => {
        const response = await authApi.login({ username });
        handleAuthResponse(response.data);
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem('user');
        localStorage.removeItem('userId');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
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
