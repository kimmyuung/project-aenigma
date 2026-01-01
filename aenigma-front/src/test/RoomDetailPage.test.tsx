import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RoomDetailPage } from '../pages/RoomDetailPage';
import { AuthContext } from '../contexts/AuthContext';
import * as client from '../api/client';

// Mock 모듈
vi.mock('../api/client', () => ({
    roomApi: {
        get: vi.fn(),
        leave: vi.fn(),
    },
}));

vi.mock('../hooks/useRoomWebSocket', () => ({
    useRoomWebSocket: vi.fn(() => ({
        isConnected: true,
        members: [
            { id: 'member-1', nickname: '방장', isHost: true, isReady: true },
            { id: 'member-2', nickname: '테스터', isHost: false, isReady: false },
            { id: 'member-3', nickname: '플레이어3', isHost: false, isReady: true },
        ],
        sendMessage: vi.fn(),
        toggleReady: vi.fn(),
        startGame: vi.fn(),
    })),
}));

// 테스트용 Mock Data
const mockUser = {
    userId: 'user-1',
    nickname: '테스터',
    username: 'GUEST_TEST',
    displayTag: '#0001',
    displayName: '테스터#0001',
};

const mockRoom = {
    id: 'room-1',
    roomCode: 'ABC123',
    title: '추리 게임방',
    status: 'WAITING' as const,
    maxPlayers: 6,
    currentPlayerCount: 3,
    hostNickname: '방장',
    isPrivate: false,
    createdAt: '2024-01-01T10:00:00',
};

// 테스트용 래퍼
const renderRoomDetailPage = (roomId = 'room-1', user = mockUser) => {
    const mockAuthContext = {
        user,
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        register: vi.fn(),
        logout: vi.fn(),
    };

    return render(
        <AuthContext.Provider value={mockAuthContext}>
            <MemoryRouter initialEntries={[`/room/${roomId}`]}>
                <Routes>
                    <Route path="/room/:roomId" element={<RoomDetailPage />} />
                    <Route path="/rooms" element={<div>Rooms Page</div>} />
                    <Route path="/game/:gameId" element={<div>Game Page</div>} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    );
};

describe('RoomDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.mocked(client.roomApi.get).mockResolvedValue({ data: mockRoom } as never);
    });

    describe('로딩 상태', () => {
        it('로딩 중 스피너 표시', () => {
            vi.mocked(client.roomApi.get).mockImplementation(() => new Promise(() => { }));

            renderRoomDetailPage();

            expect(screen.getByText('방 정보를 불러오는 중...')).toBeInTheDocument();
        });
    });

    describe('방 정보 표시', () => {
        it('방 제목과 코드가 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('추리 게임방')).toBeInTheDocument();
                expect(screen.getByText('ABC123')).toBeInTheDocument();
            });
        });

        it('참가자 수와 공개 상태가 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText(/👥 3 \/ 6/)).toBeInTheDocument();
                expect(screen.getByText('🌐 공개')).toBeInTheDocument();
            });
        });

        it('WebSocket 연결 상태가 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('🟢 연결됨')).toBeInTheDocument();
            });
        });
    });

    describe('참가자 목록', () => {
        it('모든 참가자가 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('방장')).toBeInTheDocument();
                expect(screen.getByText('테스터')).toBeInTheDocument();
                expect(screen.getByText('플레이어3')).toBeInTheDocument();
            });
        });

        it('방장에게 방장 배지 표시', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('👑 방장')).toBeInTheDocument();
            });
        });

        it('빈 슬롯이 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                // 6명 중 3명 참가 -> 3개 빈 슬롯
                const emptySlots = screen.getAllByText('대기 중...');
                expect(emptySlots.length).toBe(3);
            });
        });
    });

    describe('채팅 기능', () => {
        it('채팅 입력 폼이 존재함', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByPlaceholderText('메시지를 입력하세요...')).toBeInTheDocument();
                expect(screen.getByRole('button', { name: '전송' })).toBeInTheDocument();
            });
        });

        it('시스템 메시지가 표시됨', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('대기실에 입장하셨습니다.')).toBeInTheDocument();
            });
        });

        it('메시지 입력 및 전송', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByPlaceholderText('메시지를 입력하세요...')).toBeInTheDocument();
            });

            const input = screen.getByPlaceholderText('메시지를 입력하세요...') as HTMLInputElement;
            fireEvent.change(input, { target: { value: '안녕하세요!' } });
            expect(input.value).toBe('안녕하세요!');
        });
    });

    describe('방 코드 복사', () => {
        it('방 코드 클릭 시 클립보드 복사', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('ABC123')).toBeInTheDocument();
            });

            const copyIcon = screen.getByText('📋');
            fireEvent.click(copyIcon.closest('.room-code-badge')!);

            expect(navigator.clipboard.writeText).toHaveBeenCalledWith('ABC123');
        });
    });

    describe('준비/게임 시작 버튼', () => {
        it('일반 참가자에게 준비 버튼 표시', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /준비 완료/ })).toBeInTheDocument();
            });
        });

        it('방장에게 게임 시작 버튼 표시', async () => {
            const hostUser = { ...mockUser, nickname: '방장' };
            renderRoomDetailPage('room-1', hostUser);

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /게임 시작/ })).toBeInTheDocument();
            });
        });
    });

    describe('방 나가기', () => {
        it('방 나가기 버튼이 존재함', async () => {
            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /방 나가기/ })).toBeInTheDocument();
            });
        });

        it('방 나가기 클릭 시 API 호출', async () => {
            vi.mocked(client.roomApi.leave).mockResolvedValue({} as never);

            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /방 나가기/ })).toBeInTheDocument();
            });

            fireEvent.click(screen.getByRole('button', { name: /방 나가기/ }));

            await waitFor(() => {
                expect(client.roomApi.leave).toHaveBeenCalledWith('room-1');
            });
        });
    });

    describe('에러 상태', () => {
        it('방 로드 실패 시 에러 메시지 표시', async () => {
            vi.mocked(client.roomApi.get).mockRejectedValue(new Error('로드 실패'));

            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByText('방 정보를 불러오는데 실패했습니다.')).toBeInTheDocument();
            });
        });

        it('에러 시 방 목록 버튼 표시', async () => {
            vi.mocked(client.roomApi.get).mockRejectedValue(new Error('로드 실패'));

            renderRoomDetailPage();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: '방 목록으로' })).toBeInTheDocument();
            });
        });
    });
});
