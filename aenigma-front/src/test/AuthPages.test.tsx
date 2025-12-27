import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { RegisterPage, LoginPage } from '../pages/AuthPages';
import { AuthContext } from '../contexts/AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const mockLogin = vi.fn();
const mockRegister = vi.fn();

const renderWithAuth = (ui: React.ReactElement) => {
    const mockAuthContext = {
        user: null,
        isAuthenticated: false,
        isLoading: false,
        login: mockLogin,
        register: mockRegister,
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

describe('RegisterPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders registration form', () => {
        renderWithAuth(<RegisterPage />);

        expect(screen.getByRole('heading', { name: /AENIGMA/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/게임에서 사용할 닉네임/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /게스트로 시작하기/i })).toBeInTheDocument();
    });

    it('shows error for empty nickname', async () => {
        const user = userEvent.setup();
        renderWithAuth(<RegisterPage />);

        const submitButton = screen.getByRole('button', { name: /게스트로 시작하기/i });
        await user.click(submitButton);

        expect(screen.getByText(/닉네임을 입력해주세요/i)).toBeInTheDocument();
    });

    it('shows error for short nickname', async () => {
        const user = userEvent.setup();
        renderWithAuth(<RegisterPage />);

        const input = screen.getByPlaceholderText(/게임에서 사용할 닉네임/i);
        await user.type(input, 'A');

        const submitButton = screen.getByRole('button', { name: /게스트로 시작하기/i });
        await user.click(submitButton);

        expect(screen.getByText(/2~20자 사이/i)).toBeInTheDocument();
    });

    it('calls register on valid submission', async () => {
        const user = userEvent.setup();
        mockRegister.mockResolvedValueOnce(undefined);
        renderWithAuth(<RegisterPage />);

        const input = screen.getByPlaceholderText(/게임에서 사용할 닉네임/i);
        await user.type(input, 'TestPlayer');

        const submitButton = screen.getByRole('button', { name: /게스트로 시작하기/i });
        await user.click(submitButton);

        expect(mockRegister).toHaveBeenCalledWith('TestPlayer');
    });
});

describe('LoginPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders login form', () => {
        renderWithAuth(<LoginPage />);

        expect(screen.getByRole('heading', { name: /다시 오셨군요/i })).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/GUEST_XXXXXXXX/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /로그인/i })).toBeInTheDocument();
    });

    it('shows error for empty username', async () => {
        const user = userEvent.setup();
        renderWithAuth(<LoginPage />);

        const submitButton = screen.getByRole('button', { name: /로그인/i });
        await user.click(submitButton);

        expect(screen.getByText(/사용자명을 입력해주세요/i)).toBeInTheDocument();
    });

    it('calls login on valid submission', async () => {
        const user = userEvent.setup();
        mockLogin.mockResolvedValueOnce(undefined);
        renderWithAuth(<LoginPage />);

        const input = screen.getByPlaceholderText(/GUEST_XXXXXXXX/i);
        await user.type(input, 'GUEST_12345678');

        const submitButton = screen.getByRole('button', { name: /로그인/i });
        await user.click(submitButton);

        expect(mockLogin).toHaveBeenCalledWith('GUEST_12345678');
    });
});
