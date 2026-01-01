import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RoomsPage } from '../pages/RoomsPage';
import { AuthContext } from '../contexts/AuthContext';
import * as client from '../api/client';

// Mock 모듈
vi.mock('../api/client', () => ({
    roomApi: {
        joinable: vi.fn(),
        join: vi.fn(),
        create: vi.fn(),
    },
}));

// 테스트용 Mock Data
const mockUser = {
    userId: 'user-1',
    nickname: '테스터',
    username: 'GUEST_TEST',
    displayTag: '#0001',
    displayName: '테스터#0001',
};

const mockRooms = [
    {
        id: 'room-1',
        roomCode: 'ABC123',
        title: '즐거운 추리 게임',
        status: 'WAITING' as const,
        maxPlayers: 6,
        currentPlayerCount: 3,
        hostNickname: '방장1',
        isPrivate: false,
        createdAt: '2024-01-01T10:00:00',
    },
    {
        id: 'room-2',
        roomCode: 'DEF456',
        title: '밀실 탈출',
        status: 'PLAYING' as const,
        maxPlayers: 8,
        currentPlayerCount: 8,
        hostNickname: '방장2',
        isPrivate: false,
        createdAt: '2024-01-01T11:00:00',
    },
    {
        id: 'room-3',
        roomCode: 'GHI789',
        title: '비밀의 방',
        status: 'WAITING' as const,
        maxPlayers: 4,
        currentPlayerCount: 2,
        hostNickname: '테스터',
        isPrivate: true,
        createdAt: '2024-01-01T12:00:00',
    },
];

// 테스트용 래퍼
const renderRoomsPage = (isAuthenticated = true, user = mockUser) => {
    const mockAuthContext = {
        user: isAuthenticated ? user : null,
        isAuthenticated,
        isLoading: false,
        login: vi.fn(),
        register: vi.fn(),
        logout: vi.fn(),
    };

    return render(
        <AuthContext.Provider value={mockAuthContext}>
            <MemoryRouter initialEntries={['/rooms']}>
                <Routes>
                    <Route path="/rooms" element={<RoomsPage />} />
                    <Route path="/login" element={<div>Login Page</div>} />
                    <Route path="/room/:roomId" element={<div>Room Detail Page</div>} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    );
};

describe('RoomsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(client.roomApi.joinable).mockResolvedValue({ data: mockRooms } as never);
    });

    describe('인증 상태', () => {
        it('인증되지 않은 경우 로그인 페이지로 리다이렉트', async () => {
            renderRoomsPage(false);

            await waitFor(() => {
                expect(screen.getByText('Login Page')).toBeInTheDocument();
            });
        });
    });

    describe('방 목록 로딩', () => {
        it('로딩 중 스켈레톤 UI 표시', () => {
            vi.mocked(client.roomApi.joinable).mockImplementation(() => new Promise(() => { }));

            renderRoomsPage();

            // RoomCardSkeleton은 className으로 확인
            expect(document.querySelector('.skeleton')).toBeInTheDocument();
        });

        it('방 목록 로드 성공 후 방 카드 표시', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
                expect(screen.getByText('밀실 탈출')).toBeInTheDocument();
                expect(screen.getByText('비밀의 방')).toBeInTheDocument();
            });
        });

        it('방 목록이 비어있을 때 빈 상태 UI 표시', async () => {
            vi.mocked(client.roomApi.joinable).mockResolvedValue({ data: [] } as never);

            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('입장 가능한 방이 없습니다')).toBeInTheDocument();
            });
        });

        it('로드 실패 시 에러 메시지 표시', async () => {
            vi.mocked(client.roomApi.joinable).mockRejectedValue(new Error('로드 실패'));

            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('방 목록을 불러오는데 실패했습니다.')).toBeInTheDocument();
            });
        });
    });

    describe('검색 및 필터', () => {
        it('검색어로 방 필터링', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
            });

            const searchInput = screen.getByPlaceholderText(/방 제목, 방장, 코드로 검색/);
            fireEvent.change(searchInput, { target: { value: '밀실' } });

            expect(screen.queryByText('즐거운 추리 게임')).not.toBeInTheDocument();
            expect(screen.getByText('밀실 탈출')).toBeInTheDocument();
        });

        it('상태 필터로 방 필터링', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
            });

            // WAITING 필터 클릭
            const waitingTab = screen.getByRole('button', { name: /대기 중/ });
            fireEvent.click(waitingTab);

            expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
            expect(screen.getByText('비밀의 방')).toBeInTheDocument();
            expect(screen.queryByText('밀실 탈출')).not.toBeInTheDocument();
        });

        it('필터 초기화 버튼 동작', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
            });

            // 검색어 입력
            const searchInput = screen.getByPlaceholderText(/방 제목, 방장, 코드로 검색/);
            fireEvent.change(searchInput, { target: { value: '존재하지않는방' } });

            // 검색 결과 없음 UI
            await waitFor(() => {
                expect(screen.getByText('검색 결과가 없습니다')).toBeInTheDocument();
            });

            // 필터 초기화
            const resetButton = screen.getByRole('button', { name: /필터 초기화/ });
            fireEvent.click(resetButton);

            // 모든 방 다시 표시
            expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
        });
    });

    describe('RoomCard', () => {
        it('방 정보가 올바르게 표시됨', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
                expect(screen.getByText('방장1')).toBeInTheDocument();
                expect(screen.getByText('3 / 6')).toBeInTheDocument();
                expect(screen.getByText('ABC123')).toBeInTheDocument();
            });
        });

        it('인원 초과 방은 입장 버튼 비활성화', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('밀실 탈출')).toBeInTheDocument();
            });

            // 밀실 탈출 방은 8/8로 인원 초과 + 게임 중
            const playingButtons = screen.getAllByRole('button', { name: /게임 중/ });
            expect(playingButtons.length).toBeGreaterThan(0);
            expect(playingButtons[0]).toBeDisabled();
        });

        it('WAITING 상태 방은 입장 가능', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('즐거운 추리 게임')).toBeInTheDocument();
            });

            const enterButtons = screen.getAllByRole('button', { name: /입장하기/ });
            expect(enterButtons.length).toBeGreaterThan(0);
            expect(enterButtons[0]).not.toBeDisabled();
        });
    });

    describe('방 만들기 모달', () => {
        it('방 만들기 버튼 클릭 시 모달 열림', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            const createButton = screen.getByRole('button', { name: /방 만들기/ });
            fireEvent.click(createButton);

            expect(screen.getByText('새 방 만들기')).toBeInTheDocument();
            expect(screen.getByPlaceholderText(/예: 즐거운 추리 게임/)).toBeInTheDocument();
        });

        it('모달에서 취소 버튼 클릭 시 닫힘', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('button', { name: /방 만들기/ }));
            expect(screen.getByText('새 방 만들기')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', { name: '취소' }));
            expect(screen.queryByText('새 방 만들기')).not.toBeInTheDocument();
        });

        it('방 생성 요청 성공', async () => {
            vi.mocked(client.roomApi.create).mockResolvedValue({ data: { id: 'new-room' } } as never);

            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('button', { name: /방 만들기/ }));

            const titleInput = screen.getByPlaceholderText(/예: 즐거운 추리 게임/);
            fireEvent.change(titleInput, { target: { value: '새로운 방' } });

            fireEvent.click(screen.getByRole('button', { name: /방 만들기/ }));

            await waitFor(() => {
                expect(client.roomApi.create).toHaveBeenCalledWith({
                    title: '새로운 방',
                    maxPlayers: 6,
                    password: undefined,
                });
            });
        });
    });

    describe('코드로 입장 모달', () => {
        it('코드로 입장 버튼 클릭 시 모달 열림', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            const joinButton = screen.getByRole('button', { name: /코드로 입장/ });
            fireEvent.click(joinButton);

            expect(screen.getByText('코드로 입장')).toBeInTheDocument();
            expect(screen.getByPlaceholderText('ABC123')).toBeInTheDocument();
        });

        it('방 코드 입력 시 대문자로 변환', async () => {
            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('button', { name: /코드로 입장/ }));

            const codeInput = screen.getByPlaceholderText('ABC123') as HTMLInputElement;
            fireEvent.change(codeInput, { target: { value: 'abc123' } });

            expect(codeInput.value).toBe('ABC123');
        });

        it('코드 입장 요청 성공 시 방 상세 페이지로 이동', async () => {
            vi.mocked(client.roomApi.join).mockResolvedValue({ data: { id: 'joined-room' } } as never);

            renderRoomsPage();

            await waitFor(() => {
                expect(screen.getByText('게임 방 목록')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('button', { name: /코드로 입장/ }));

            const codeInput = screen.getByPlaceholderText('ABC123');
            fireEvent.change(codeInput, { target: { value: 'XYZ789' } });

            const submitButton = screen.getByRole('button', { name: '입장하기' });
            fireEvent.click(submitButton);

            await waitFor(() => {
                expect(client.roomApi.join).toHaveBeenCalledWith({
                    roomCode: 'XYZ789',
                    password: undefined,
                });
            });
        });
    });
});
