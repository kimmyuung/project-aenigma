import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

import { BrowserRouter } from 'react-router-dom';
import { HomePage } from '../pages/HomePage';
import { AuthContext } from '../contexts/AuthContext';

// 테스트용 래퍼
const renderWithProviders = (ui: React.ReactElement, { isAuthenticated = false } = {}) => {
    const mockAuthContext = {
        user: isAuthenticated ? { userId: '1', nickname: 'TestUser', username: 'GUEST_TEST', displayTag: '#0001', displayName: 'TestUser#0001' } : null,
        isAuthenticated,
        isLoading: false,
        login: vi.fn(),
        register: vi.fn(),
        logout: vi.fn(),
    };

    return render(
        <AuthContext.Provider value={mockAuthContext}>
            <BrowserRouter>
                {ui}
            </BrowserRouter>
        </AuthContext.Provider>
    );
};

describe('HomePage', () => {
    it('renders hero section with title', () => {
        renderWithProviders(<HomePage />);

        expect(screen.getByRole('heading', { name: /AENIGMA/i })).toBeInTheDocument();
        expect(screen.getByText(/친구들과 함께 즐기는/i)).toBeInTheDocument();
    });

    it('shows login/register buttons when not authenticated', () => {
        renderWithProviders(<HomePage />, { isAuthenticated: false });

        // 로그인/회원가입 링크 존재 여부 확인
        const links = screen.getAllByRole('link');
        const registerLink = links.find(link => link.getAttribute('href') === '/register');
        const loginLink = links.find(link => link.getAttribute('href') === '/login');

        expect(registerLink).toBeInTheDocument();
        expect(loginLink).toBeInTheDocument();
    });

    it('shows game start button when authenticated', () => {
        renderWithProviders(<HomePage />, { isAuthenticated: true });

        expect(screen.getByRole('link', { name: /게임 시작하기/i })).toBeInTheDocument();
    });

    it('renders all feature cards', () => {
        renderWithProviders(<HomePage />);

        expect(screen.getByText(/역할 기반 게임플레이/i)).toBeInTheDocument();
        expect(screen.getByText(/실시간 채팅/i)).toBeInTheDocument();
        expect(screen.getByText(/Discord 연동/i)).toBeInTheDocument();
        expect(screen.getByText(/실시간 진행/i)).toBeInTheDocument();
    });

    it('renders game steps section', () => {
        renderWithProviders(<HomePage />);

        expect(screen.getByText(/방 만들기/i)).toBeInTheDocument();
        expect(screen.getByText(/역할 배정/i)).toBeInTheDocument();
        expect(screen.getByText(/조사 단계/i)).toBeInTheDocument();
        expect(screen.getByText(/최종 투표/i)).toBeInTheDocument();
    });
});
