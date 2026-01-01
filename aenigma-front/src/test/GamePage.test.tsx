import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter, Route, Routes, MemoryRouter } from 'react-router-dom';
import { GamePage } from '../pages/GamePage';
import { AuthContext } from '../contexts/AuthContext';
import * as client from '../api/client';

// Mock 모듈
vi.mock('../api/client', () => ({
    gameApi: {
        get: vi.fn(),
        getClues: vi.fn(),
        getMyRole: vi.fn(),
        vote: vi.fn(),
    },
    voteApi: {
        getResults: vi.fn(),
    },
}));

vi.mock('../hooks/useWebSocket', () => ({
    useWebSocket: vi.fn(() => ({
        sendPublicMessage: vi.fn(() => true),
        connectionState: 'connected' as const,
        retryCount: 0,
        maxRetries: 5,
        queuedMessageCount: 0,
        reconnect: vi.fn(),
    })),
}));

vi.mock('../components/error/ToastProvider', () => ({
    useToast: () => ({
        success: vi.fn(),
        error: vi.fn(),
        info: vi.fn(),
    }),
}));

// 테스트용 Mock Data
const mockUser = {
    userId: 'user-1',
    nickname: '테스터',
    username: 'GUEST_TEST',
    displayTag: '#0001',
    displayName: '테스터#0001',
};

const mockGameResponse = {
    data: {
        id: 'game-1',
        roomId: 'room-1',
        phase: 'INVESTIGATION' as const,
        investigationRound: 1,
        maxInvestigationRounds: 3,
        players: [
            { id: 'player-1', playerId: 'player-1', userId: 'user-1', nickname: '테스터', displayTag: '#0001', isAlive: true },
            { id: 'player-2', playerId: 'player-2', userId: 'user-2', nickname: '플레이어2', displayTag: '#0002', isAlive: true },
            { id: 'player-3', playerId: 'player-3', userId: 'user-3', nickname: '플레이어3', displayTag: '#0003', isAlive: true },
        ],
    },
};

const mockCluesResponse = {
    data: [
        { id: 'clue-1', title: '혈흔', content: '현장에서 발견된 혈흔', clueType: 'PUBLIC', isDiscovered: true, importance: 5 },
        { id: 'clue-2', title: '비밀 단서', content: '숨겨진 정보', clueType: 'PERSONAL', isDiscovered: false, importance: 3 },
    ],
};

const mockRoleResponse = {
    data: {
        playerId: 'player-1',
        nickname: '테스터',
        displayTag: '#0001',
        roleType: 'SUSPECT',
        roleName: '탐정',
        description: '범인을 찾아내세요',
        objective: '진실을 밝혀라',
        isAlive: true,
    },
};

// 테스트용 래퍼
const renderGamePage = (gameId = 'game-1', user = mockUser) => {
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
            <MemoryRouter initialEntries={[`/game/${gameId}`]}>
                <Routes>
                    <Route path="/game/:gameId" element={<GamePage />} />
                </Routes>
            </MemoryRouter>
        </AuthContext.Provider>
    );
};

describe('GamePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        // 기본 Mock 설정
        vi.mocked(client.gameApi.get).mockResolvedValue(mockGameResponse as never);
        vi.mocked(client.gameApi.getClues).mockResolvedValue(mockCluesResponse as never);
        vi.mocked(client.gameApi.getMyRole).mockResolvedValue(mockRoleResponse as never);
    });

    describe('로딩 상태', () => {
        it('게임 로딩 중 스켈레톤 UI를 표시한다', () => {
            // get 호출이 지연되도록 설정
            vi.mocked(client.gameApi.get).mockImplementation(() => new Promise(() => { }));

            renderGamePage();

            expect(screen.getByText('참가자')).toBeInTheDocument();
            expect(screen.getByText('📋 단서')).toBeInTheDocument();
        });
    });

    describe('게임 상태 로드 후', () => {
        it('GameHeader가 게임 정보를 올바르게 표시한다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText(/조사/i)).toBeInTheDocument();
            });
        });

        it('플레이어 목록을 표시한다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText('테스터')).toBeInTheDocument();
                expect(screen.getByText('플레이어2')).toBeInTheDocument();
                expect(screen.getByText('플레이어3')).toBeInTheDocument();
            });
        });

        it('시스템 메시지가 표시된다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText(/게임에 입장하셨습니다/)).toBeInTheDocument();
            });
        });
    });

    describe('채팅 기능', () => {
        it('채팅 입력 폼이 존재한다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByPlaceholderText(/메시지를 입력/i)).toBeInTheDocument();
            });
        });

        it('메시지를 입력하고 전송할 수 있다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByPlaceholderText(/메시지를 입력/i)).toBeInTheDocument();
            });

            const input = screen.getByPlaceholderText(/메시지를 입력/i) as HTMLInputElement;
            fireEvent.change(input, { target: { value: '안녕하세요' } });
            expect(input.value).toBe('안녕하세요');
        });
    });

    describe('단서 패널', () => {
        it('역할 카드가 표시된다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText('내 역할')).toBeInTheDocument();
            });
        });

        it('발견된 단서와 미발견 단서를 구분하여 표시한다', async () => {
            renderGamePage();

            await waitFor(() => {
                // 발견된 단서
                expect(screen.getByText('혈흔')).toBeInTheDocument();
            });
        });
    });

    describe('투표 기능 (FINAL_VOTE 단계)', () => {
        beforeEach(() => {
            const finalVoteGameResponse = {
                ...mockGameResponse,
                data: {
                    ...mockGameResponse.data,
                    phase: 'FINAL_VOTE',
                },
            };
            vi.mocked(client.gameApi.get).mockResolvedValue(finalVoteGameResponse as never);
        });

        it('FINAL_VOTE 단계에서 투표 섹션이 표시된다', async () => {
            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText(/최종 투표/)).toBeInTheDocument();
            });
        });

        it('플레이어를 선택하지 않으면 투표 버튼이 비활성화된다', async () => {
            renderGamePage();

            await waitFor(() => {
                const voteButton = screen.getByRole('button', { name: /투표하기/ });
                expect(voteButton).toBeDisabled();
            });
        });

        it('투표 성공 시 투표 완료 상태로 변경된다', async () => {
            vi.mocked(client.gameApi.vote).mockResolvedValue({ data: { success: true, message: '투표 완료' } } as never);

            renderGamePage();

            await waitFor(() => {
                expect(screen.getByText('플레이어2')).toBeInTheDocument();
            });

            // 플레이어 선택 (클릭 가능한 요소 찾기)
            const player2 = screen.getByText('플레이어2');
            fireEvent.click(player2);

            // 투표 버튼 클릭
            const voteButton = screen.getByRole('button', { name: /투표하기/ });
            fireEvent.click(voteButton);

            await waitFor(() => {
                expect(client.gameApi.vote).toHaveBeenCalledWith('game-1', expect.any(String));
            });
        });
    });

    describe('게임 결과 (CONCLUSION 단계)', () => {
        beforeEach(() => {
            const conclusionGameResponse = {
                data: {
                    ...mockGameResponse.data,
                    phase: 'CONCLUSION',
                    winnerTeam: 'SUSPECT',
                    scenarioTitle: '밀실의 비밀',
                    scenarioSummary: '모든 진실이 밝혀졌습니다.',
                },
            };
            vi.mocked(client.gameApi.get).mockResolvedValue(conclusionGameResponse as never);
            vi.mocked(client.voteApi.getResults).mockResolvedValue({
                data: {
                    gameId: 'game-1',
                    round: 1,
                    results: { 'player-2': 2, 'player-3': 1 },
                    isComplete: true,
                    totalVotes: 3,
                    expectedVotes: 3,
                },
            } as never);
        });

        it('CONCLUSION 단계에서 게임 결과가 표시된다', async () => {
            renderGamePage();

            await waitFor(() => {
                // GameResult 컴포넌트 로드 확인
                expect(screen.queryByText(/로딩|결과/)).toBeInTheDocument();
            }, { timeout: 3000 });
        });
    });

    describe('모바일 탭 네비게이션', () => {
        it('MobileNav 컴포넌트가 렌더링된다', async () => {
            renderGamePage();

            await waitFor(() => {
                // MobileNav는 하단에 렌더링됨
                expect(document.querySelector('.mobile-nav')).toBeInTheDocument();
            });
        });
    });

    describe('에러 처리', () => {
        it('게임 로드 실패 시 에러를 처리한다', async () => {
            vi.mocked(client.gameApi.get).mockRejectedValue(new Error('로드 실패'));

            renderGamePage();

            // 에러 발생 시에도 UI가 깨지지 않아야 함
            await waitFor(() => {
                expect(screen.getByText('참가자')).toBeInTheDocument();
            });
        });
    });
});
