import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { gameApi, voteApi, type Clue } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useWebSocket, type ChatMessage, type ConnectionState } from '../hooks/useWebSocket';
import { useToast } from '../components/error/ToastProvider';
import { PlayerListSkeleton, ClueListSkeleton, ChatSkeleton } from '../components/Skeleton';
import { MobileNav, type MobileTabType } from '../components/MobileNav';
import { GameResult } from '../components/GameResult';
import {
    GameHeader,
    PlayerList,
    ChatPanel,
    CluePanel,
    VotePanel,
    RoleModal,
    ConnectionStatus,
    type GamePhase,
    type GameState,
    type RoleDetail,
    type ChatMessage as GameChatMessage,
} from '../components/game';
import './GamePage.css';

export function GamePage() {
    const { gameId } = useParams<{ gameId: string }>();
    const { user } = useAuth();
    const navigate = useNavigate();

    const [game, setGame] = useState<GameState | null>(null);
    const [messages, setMessages] = useState<GameChatMessage[]>([]);
    const [newMessage, setNewMessage] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
    const [votedPlayer, setVotedPlayer] = useState<string | null>(null);
    const [wsConnectionState, setWsConnectionState] = useState<ConnectionState>('connecting');
    const toast = useToast();

    // 추가된 상태
    const [clues, setClues] = useState<Clue[]>([]);
    const [roleDetail, setRoleDetail] = useState<RoleDetail | null>(null);
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [isVoting, setIsVoting] = useState(false);
    const [voteError, setVoteError] = useState<string | null>(null);

    // 게임 결과 상태
    const [winnerTeam, setWinnerTeam] = useState<'CRIMINAL' | 'SUSPECT' | 'DETECTIVE' | null>(null);
    const [voteResults, setVoteResults] = useState<{ targetId: string; targetNickname: string; voteCount: number }[]>([]);
    const [scenarioInfo, setScenarioInfo] = useState<{ title?: string; summary?: string }>({});
    const [isLoadingResults, setIsLoadingResults] = useState(false);

    // 모바일 탭 상태
    const [mobileTab, setMobileTab] = useState<MobileTabType>('chat');
    const [unreadMessages, setUnreadMessages] = useState(0);

    // WebSocket 메시지 수신 핸들러
    const handleWebSocketMessage = useCallback((message: ChatMessage) => {
        const gameMessage: GameChatMessage = {
            id: message.id,
            gameId: message.gameId,
            senderId: message.senderId,
            senderNickname: message.senderNickname,
            content: message.content,
            type: message.type as GameChatMessage['type'],
            timestamp: message.timestamp,
        };
        setMessages(prev => [...prev, gameMessage]);
        if (mobileTab !== 'chat' && message.senderId !== user?.userId) {
            setUnreadMessages(prev => prev + 1);
        }
    }, [mobileTab, user?.userId]);

    // WebSocket 연결
    const {
        sendPublicMessage,
        connectionState,
        retryCount,
        maxRetries,
        queuedMessageCount,
        reconnect,
    } = useWebSocket({
        gameId: gameId || '',
        userId: user?.userId || '',
        onMessage: handleWebSocketMessage,
        onConnect: () => {
            setWsConnectionState('connected');
            toast.success('연결됨', '채팅 서버에 연결되었습니다.');
        },
        onDisconnect: () => setWsConnectionState('disconnected'),
        onConnectionStateChange: setWsConnectionState,
        onError: (error) => toast.error('연결 오류', error),
    });

    // 메시지 큐 이벤트 처리
    useEffect(() => {
        const handleQueueFlushed = (e: CustomEvent) => {
            toast.success('메시지 전송 완료', `${e.detail.count}개의 대기 메시지가 전송되었습니다.`);
        };
        const handleMessageQueued = () => {
            toast.info('메시지 대기 중', '연결 복구 후 자동으로 전송됩니다.');
        };
        const handleSendFailed = () => {
            toast.error('전송 실패', '메시지를 보낼 수 없습니다.');
        };

        window.addEventListener('websocket-queue-flushed', handleQueueFlushed as EventListener);
        window.addEventListener('websocket-message-queued', handleMessageQueued as EventListener);
        window.addEventListener('websocket-send-failed', handleSendFailed as EventListener);

        return () => {
            window.removeEventListener('websocket-queue-flushed', handleQueueFlushed as EventListener);
            window.removeEventListener('websocket-message-queued', handleMessageQueued as EventListener);
            window.removeEventListener('websocket-send-failed', handleSendFailed as EventListener);
        };
    }, [toast]);

    useEffect(() => {
        if (gameId) {
            loadGameState();
            loadClues();
            loadMyRole();
        }
    }, [gameId]);

    const loadGameState = async () => {
        try {
            setIsLoading(true);
            const response = await gameApi.get(gameId!);
            setGame({
                id: response.data.id,
                phase: response.data.phase as GamePhase,
                round: response.data.investigationRound || 1,
                maxRounds: response.data.maxInvestigationRounds || 3,
                players: response.data.players || [],
                myRole: response.data.players?.find(p => p.id === user?.userId)?.role,
            });

            setMessages([{
                id: 'sys-1',
                gameId: gameId!,
                senderId: 'system',
                senderNickname: '시스템',
                content: '게임에 입장하셨습니다. 역할을 확인해주세요.',
                type: 'SYSTEM',
                timestamp: new Date().toISOString(),
            }]);
        } catch (err) {
            console.error('게임 로드 실패', err);
        } finally {
            setIsLoading(false);
        }
    };

    const loadClues = async () => {
        try {
            const response = await gameApi.getClues(gameId!);
            setClues(response.data);
        } catch (err) {
            console.error('단서 로드 실패', err);
        }
    };

    const loadMyRole = async () => {
        try {
            const response = await gameApi.getMyRole(gameId!);
            setRoleDetail(response.data);
        } catch (err) {
            console.error('역할 로드 실패', err);
        }
    };

    // 게임 결과 데이터 로드
    const loadGameResults = useCallback(async () => {
        if (!gameId || !game || isLoadingResults) return;

        try {
            setIsLoadingResults(true);

            // 1. 게임 정보 재조회 (최신 상태 반영)
            const gameResponse = await gameApi.get(gameId);
            const gameData = gameResponse.data;

            // winnerTeam 설정
            if (gameData.winnerTeam) {
                setWinnerTeam(gameData.winnerTeam as 'CRIMINAL' | 'SUSPECT' | 'DETECTIVE');
            }

            // 시나리오 정보 설정
            if (gameData.scenarioTitle || gameData.scenarioSummary) {
                setScenarioInfo({
                    title: gameData.scenarioTitle,
                    summary: gameData.scenarioSummary,
                });
            }

            // 2. 투표 결과 로드
            try {
                const round = gameData.investigationRound || game.round || 1;
                const voteResponse = await voteApi.getResults(gameId, round);
                const results = voteResponse.data.results;

                // UUID -> 닉네임 매핑
                const formattedResults = Object.entries(results).map(([playerId, count]) => {
                    const player = gameData.players?.find(
                        (p: { userId?: string; id?: string }) => p.userId === playerId || p.id === playerId
                    );
                    return {
                        targetId: playerId,
                        targetNickname: player?.nickname || 'Unknown',
                        voteCount: count as number,
                    };
                }).sort((a, b) => b.voteCount - a.voteCount);

                setVoteResults(formattedResults);
            } catch (voteErr) {
                console.error('투표 결과 로드 실패', voteErr);
                // 투표 결과가 없어도 결과 화면은 표시
            }

            toast.success('결과 로드 완료', '게임 결과를 불러왔습니다.');
        } catch (err) {
            console.error('게임 결과 로드 실패', err);
            toast.error('결과 로드 실패', '게임 결과를 불러오지 못했습니다.');
        } finally {
            setIsLoadingResults(false);
        }
    }, [gameId, game, isLoadingResults, toast]);

    // 게임 종료 단계 진입 시 결과 로드
    useEffect(() => {
        if (game && (game.phase === 'CONCLUSION' || game.phase === 'FINISHED')) {
            loadGameResults();
        }
    }, [game?.phase]);

    const handleSendMessage = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMessage.trim()) return;

        const sent = sendPublicMessage(newMessage);
        if (sent) {
            const message: GameChatMessage = {
                id: `temp-${Date.now()}`,
                gameId: gameId!,
                senderId: user?.userId || '',
                senderNickname: user?.nickname || '나',
                content: newMessage,
                type: 'PUBLIC',
                timestamp: new Date().toISOString(),
            };
            setMessages(prev => [...prev, message]);
        }
        setNewMessage('');
    };

    const handleVote = async () => {
        if (!selectedPlayer || isVoting || votedPlayer) return;

        try {
            setIsVoting(true);
            setVoteError(null);
            const response = await gameApi.vote(gameId!, selectedPlayer);

            if (response.data.success) {
                setVotedPlayer(selectedPlayer);
                setMessages(prev => [...prev, {
                    id: `sys-vote-${Date.now()}`,
                    gameId: gameId!,
                    senderId: 'system',
                    senderNickname: '시스템',
                    content: '✅ 투표가 완료되었습니다.',
                    type: 'SYSTEM',
                    timestamp: new Date().toISOString(),
                }]);
            }
        } catch (err) {
            console.error('투표 실패', err);
            setVoteError('투표에 실패했습니다. 다시 시도해주세요.');
        } finally {
            setIsVoting(false);
        }
    };

    const handleMobileTabChange = (tab: MobileTabType) => {
        setMobileTab(tab);
        if (tab === 'chat') {
            setUnreadMessages(0);
        }
    };

    // Loading state
    if (isLoading || !game) {
        return (
            <div className="game-page">
                <header className="game-header">
                    <div className="game-info">
                        <div className="skeleton" style={{ width: '120px', height: '32px', borderRadius: '8px' }} />
                        <div className="skeleton" style={{ width: '80px', height: '32px', borderRadius: '8px' }} />
                    </div>
                    <div className="skeleton" style={{ width: '150px', height: '40px', borderRadius: '8px' }} />
                </header>
                <div className="game-content">
                    <aside className="players-panel">
                        <h3>참가자</h3>
                        <PlayerListSkeleton count={5} />
                    </aside>
                    <main className="game-main">
                        <div className="chat-section">
                            <ChatSkeleton count={6} />
                        </div>
                    </main>
                    <aside className="clues-panel">
                        <div className="skeleton" style={{ width: '100%', height: '100px', borderRadius: '12px', marginBottom: '1rem' }} />
                        <h3>📋 단서</h3>
                        <ClueListSkeleton count={3} />
                    </aside>
                </div>
                <MobileNav activeTab={mobileTab} onTabChange={setMobileTab} />
            </div>
        );
    }

    return (
        <div className="game-page">
            {/* Game Header */}
            <GameHeader
                phase={game.phase}
                round={game.round}
                maxRounds={game.maxRounds}
                myRole={game.myRole}
                wsStatus={wsConnectionState === 'connected' ? 'connected' : wsConnectionState === 'connecting' ? 'connecting' : 'disconnected'}
                onRoleClick={() => setShowRoleModal(true)}
            />

            <div className={`game-content mobile-${mobileTab}`}>
                {/* Players Panel */}
                <PlayerList
                    players={game.players}
                    phase={game.phase}
                    selectedPlayer={selectedPlayer}
                    votedPlayer={votedPlayer}
                    onPlayerSelect={setSelectedPlayer}
                />

                {/* Main Game Area */}
                <main className="game-main">
                    {/* Chat Section */}
                    <ChatPanel
                        messages={messages}
                        userId={user?.userId}
                        newMessage={newMessage}
                        onNewMessageChange={setNewMessage}
                        onSendMessage={handleSendMessage}
                    />

                    {/* Vote Section (Final Vote Phase) */}
                    {game.phase === 'FINAL_VOTE' && (
                        <VotePanel
                            selectedPlayer={selectedPlayer}
                            votedPlayer={votedPlayer}
                            isVoting={isVoting}
                            voteError={voteError}
                            onVote={handleVote}
                        />
                    )}

                    {/* Game Result Section */}
                    {(game.phase === 'CONCLUSION' || game.phase === 'FINISHED') && (
                        isLoadingResults ? (
                            <div className="game-result-loading">
                                <div className="loading-spinner" />
                                <p>게임 결과를 불러오는 중...</p>
                            </div>
                        ) : (
                            <GameResult
                                winnerTeam={winnerTeam}
                                players={game.players}
                                voteResults={voteResults}
                                myRole={roleDetail ?? undefined}
                                scenarioTitle={scenarioInfo.title}
                                scenarioSummary={scenarioInfo.summary}
                                onLeave={() => navigate('/rooms')}
                            />
                        )
                    )}
                </main>

                {/* Clues Panel */}
                <CluePanel
                    clues={clues}
                    myRole={game.myRole}
                    roleDetail={roleDetail}
                    onRoleCardClick={() => setShowRoleModal(true)}
                />
            </div>

            {/* WebSocket Connection Status */}
            <ConnectionStatus
                state={connectionState}
                retryCount={retryCount}
                maxRetries={maxRetries}
                queuedMessageCount={queuedMessageCount}
                onReconnect={reconnect}
            />

            {/* Footer */}
            <footer className="game-footer">
                <div className="footer-content">
                    <div className="footer-section">
                        <h4>🎭 AENIGMA</h4>
                        <p>온라인 머더미스터리 게임</p>
                    </div>
                    <div className="footer-section">
                        <h4>게임 정보</h4>
                        <p>친구들과 함께 추리하며 범인을 찾아내세요!</p>
                    </div>
                    <div className="footer-section">
                        <h4>개발자</h4>
                        <a href="https://github.com/kimmyuung" target="_blank" rel="noopener noreferrer">
                            🔗 GitHub: kimmyuung
                        </a>
                    </div>
                </div>
                <div className="footer-bottom">
                    <p>© 2024 AENIGMA. All rights reserved.</p>
                </div>
            </footer>

            {/* Mobile Navigation */}
            <MobileNav
                activeTab={mobileTab}
                onTabChange={handleMobileTabChange}
                unreadMessages={unreadMessages}
            />

            {/* Role Modal */}
            {showRoleModal && roleDetail && (
                <RoleModal
                    roleDetail={roleDetail}
                    onClose={() => setShowRoleModal(false)}
                />
            )}
        </div>
    );
}
