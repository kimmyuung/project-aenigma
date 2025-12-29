import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { gameApi, type GamePlayer, type Clue, type RoleDetail } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useWebSocket, type ChatMessage } from '../hooks/useWebSocket';
import { PlayerListSkeleton, ClueListSkeleton, ChatSkeleton } from '../components/Skeleton';
import { MobileNav, type MobileTabType } from '../components/MobileNav';
import './GamePage.css';

type GamePhase = 'INTRO' | 'LOBBY' | 'INVESTIGATION' | 'FINAL_VOTE' | 'CONCLUSION' | 'FINISHED';

interface GameState {
    id: string;
    phase: GamePhase;
    round: number;
    maxRounds: number;
    players: GamePlayer[];
    myRole?: string;
}

export function GamePage() {
    const { gameId } = useParams<{ gameId: string }>();
    const { user } = useAuth();
    const chatContainerRef = useRef<HTMLDivElement>(null);

    const [game, setGame] = useState<GameState | null>(null);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [newMessage, setNewMessage] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
    const [votedPlayer, setVotedPlayer] = useState<string | null>(null);
    const [wsStatus, setWsStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');

    // 추가된 상태
    const [clues, setClues] = useState<Clue[]>([]);
    const [roleDetail, setRoleDetail] = useState<RoleDetail | null>(null);
    const [showRoleModal, setShowRoleModal] = useState(false);
    const [isVoting, setIsVoting] = useState(false);
    const [voteError, setVoteError] = useState<string | null>(null);

    // 모바일 탭 상태
    const [mobileTab, setMobileTab] = useState<MobileTabType>('chat');
    const [unreadMessages, setUnreadMessages] = useState(0);

    // WebSocket 메시지 수신 핸들러
    const handleWebSocketMessage = useCallback((message: ChatMessage) => {
        setMessages(prev => [...prev, message]);
        // 모바일에서 채팅 탭이 아닐 때 읽지 않은 메시지 카운트
        if (mobileTab !== 'chat' && message.senderId !== user?.userId) {
            setUnreadMessages(prev => prev + 1);
        }
    }, [mobileTab, user?.userId]);

    // WebSocket 연결
    const {
        sendPublicMessage,
    } = useWebSocket({
        gameId: gameId || '',
        userId: user?.userId || '',
        onMessage: handleWebSocketMessage,
        onConnect: () => setWsStatus('connected'),
        onDisconnect: () => setWsStatus('disconnected'),
    });

    useEffect(() => {
        if (gameId) {
            loadGameState();
            loadClues();
            loadMyRole();
        }
    }, [gameId]);

    useEffect(() => {
        // 채팅 스크롤 자동 하단
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [messages]);

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

            // 시스템 메시지 추가
            setMessages([
                {
                    id: 'sys-1',
                    gameId: gameId!,
                    senderId: 'system',
                    senderNickname: '시스템',
                    content: '게임에 입장하셨습니다. 역할을 확인해주세요.',
                    type: 'SYSTEM',
                    timestamp: new Date().toISOString(),
                },
            ]);
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

    const handleSendMessage = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMessage.trim()) return;

        // WebSocket으로 전송
        const sent = sendPublicMessage(newMessage);

        if (sent) {
            // 낙관적 UI 업데이트 (서버 응답 전에 표시)
            const message: ChatMessage = {
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
                // 투표 완료 시스템 메시지
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

    const getClueTypeClass = (clueType: string): string => {
        switch (clueType) {
            case 'PUBLIC': return 'public';
            case 'PERSONAL': return 'private';
            case 'HIDDEN': return 'secret';
            default: return '';
        }
    };

    const getClueTypeLabel = (clueType: string): string => {
        switch (clueType) {
            case 'PUBLIC': return '🔍 공개 단서';
            case 'PERSONAL': return '🎭 개인 단서';
            case 'HIDDEN': return '🔒 비밀 단서';
            default: return '📋 단서';
        }
    };

    const getPhaseTitle = (phase: GamePhase): string => {
        switch (phase) {
            case 'INTRO': return '🎭 도입';
            case 'LOBBY': return '📜 역할 숙지';
            case 'INVESTIGATION': return '🔍 조사 시간';
            case 'FINAL_VOTE': return '⚖️ 최종 투표';
            case 'CONCLUSION': return '🎬 결과 발표';
            case 'FINISHED': return '🏆 게임 종료';
            default: return phase;
        }
    };

    const getRoleEmoji = (role?: string): string => {
        switch (role) {
            case 'CRIMINAL': return '🔪';
            case 'DETECTIVE': return '🔍';
            case 'CITIZEN': return '👤';
            default: return '❓';
        }
    };

    // 모바일 탭 변경 핸들러
    const handleMobileTabChange = (tab: MobileTabType) => {
        setMobileTab(tab);
        if (tab === 'chat') {
            setUnreadMessages(0);
        }
    };

    if (isLoading || !game) {
        return (
            <div className="game-page">
                {/* 스켈레톤 로딩 UI */}
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
            <header className="game-header">
                <div className="game-info">
                    <span className="phase-badge">{getPhaseTitle(game.phase)}</span>
                    {game.phase === 'INVESTIGATION' && (
                        <span className="round-badge">라운드 {game.round} / {game.maxRounds}</span>
                    )}
                    <span className={`ws-status ${wsStatus}`}>
                        {wsStatus === 'connected' ? '🟢' : wsStatus === 'connecting' ? '🟡' : '🔴'}
                    </span>
                </div>
                <div className="my-role" onClick={() => setShowRoleModal(true)} style={{ cursor: 'pointer' }}>
                    <span className="role-emoji">{getRoleEmoji(game.myRole)}</span>
                    <span className="role-name">{game.myRole || '역할 미정'}</span>
                    <span className="role-hint">ℹ️</span>
                </div>
            </header>

            <div className={`game-content mobile-${mobileTab}`}>
                {/* Players Panel */}
                <aside className="players-panel">
                    <h3>참가자</h3>
                    <div className="players-list">
                        {game.players.map((player) => (
                            <div
                                key={player.id}
                                className={`player-item ${!player.isAlive ? 'eliminated' : ''} ${selectedPlayer === player.id ? 'selected' : ''}`}
                                onClick={() => player.isAlive && game.phase === 'FINAL_VOTE' && setSelectedPlayer(player.id)}
                            >
                                <div className="player-avatar">
                                    {player.nickname.charAt(0).toUpperCase()}
                                </div>
                                <span className="player-name">{player.nickname}</span>
                                {!player.isAlive && <span className="eliminated-badge">💀</span>}
                                {votedPlayer === player.id && <span className="voted-badge">✓</span>}
                            </div>
                        ))}
                    </div>
                </aside>

                {/* Main Game Area */}
                <main className="game-main">
                    {/* Chat Section */}
                    <div className="chat-section">
                        <div className="chat-messages" ref={chatContainerRef}>
                            {messages.map((msg) => (
                                <div
                                    key={msg.id}
                                    className={`chat-message ${msg.type.toLowerCase()} ${msg.senderId === user?.userId ? 'mine' : ''}`}
                                >
                                    {msg.type === 'SYSTEM' ? (
                                        <div className="system-message">{msg.content}</div>
                                    ) : (
                                        <>
                                            <span className="sender">{msg.senderNickname}</span>
                                            <span className="content">{msg.content}</span>
                                        </>
                                    )}
                                </div>
                            ))}
                        </div>

                        <form className="chat-input-form" onSubmit={handleSendMessage}>
                            <input
                                type="text"
                                className="chat-input"
                                placeholder="메시지를 입력하세요..."
                                value={newMessage}
                                onChange={(e) => setNewMessage(e.target.value)}
                            />
                            <button type="submit" className="btn btn-primary">
                                전송
                            </button>
                        </form>
                    </div>

                    {/* Vote Section (Final Vote Phase) */}
                    {game.phase === 'FINAL_VOTE' && (
                        <div className="vote-section">
                            <h3>⚖️ 최종 투표</h3>
                            <p>범인이라고 생각하는 사람을 선택하세요.</p>
                            {voteError && <p className="error-message">{voteError}</p>}
                            <div className="vote-actions">
                                <button
                                    className="btn btn-primary btn-lg"
                                    onClick={handleVote}
                                    disabled={!selectedPlayer || !!votedPlayer || isVoting}
                                >
                                    {isVoting ? '투표 중...' : votedPlayer ? '투표 완료' : '투표하기'}
                                </button>
                            </div>
                        </div>
                    )}

                    {/* Game Result Section (Conclusion/Finished Phase) */}
                    {(game.phase === 'CONCLUSION' || game.phase === 'FINISHED') && (
                        <div className="result-section">
                            <div className="result-header">
                                <span className="result-icon">🏆</span>
                                <h2>게임 결과</h2>
                            </div>

                            <div className="result-card winner">
                                <h3>🎉 시민팀 승리!</h3>
                                <p>범인을 성공적으로 찾아냈습니다.</p>
                            </div>

                            <div className="roles-reveal">
                                <h3>📋 역할 공개</h3>
                                <div className="roles-list">
                                    {game.players.map((player) => (
                                        <div key={player.id} className="role-reveal-item">
                                            <div className="player-avatar">
                                                {player.nickname.charAt(0).toUpperCase()}
                                            </div>
                                            <div className="role-reveal-info">
                                                <span className="player-name">{player.nickname}</span>
                                                <span className="player-role">
                                                    {getRoleEmoji(player.role)} {player.role || '역할 미정'}
                                                </span>
                                            </div>
                                            {!player.isAlive && <span className="eliminated-badge">💀</span>}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="game-stats">
                                <h3>📊 게임 통계</h3>
                                <div className="stats-grid">
                                    <div className="stat-item">
                                        <span className="stat-label">게임 시간</span>
                                        <span className="stat-value">15:32</span>
                                    </div>
                                    <div className="stat-item">
                                        <span className="stat-label">총 라운드</span>
                                        <span className="stat-value">{game.maxRounds}</span>
                                    </div>
                                    <div className="stat-item">
                                        <span className="stat-label">참가자</span>
                                        <span className="stat-value">{game.players.length}명</span>
                                    </div>
                                    <div className="stat-item">
                                        <span className="stat-label">생존자</span>
                                        <span className="stat-value">
                                            {game.players.filter(p => p.isAlive).length}명
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <div className="result-actions">
                                <button className="btn btn-primary btn-lg" onClick={() => window.location.href = '/rooms'}>
                                    🎮 새 게임 시작
                                </button>
                            </div>
                        </div>
                    )}
                </main>

                {/* Clues Panel */}
                <aside className="clues-panel">
                    {/* 내 역할 카드 */}
                    <div className="role-card" onClick={() => setShowRoleModal(true)}>
                        <div className="role-card-header">
                            <span className="role-emoji-lg">{getRoleEmoji(game.myRole)}</span>
                            <div className="role-card-info">
                                <span className="role-label">내 역할</span>
                                <span className="role-name-lg">{roleDetail?.roleName || game.myRole || '미정'}</span>
                            </div>
                            <span className="role-arrow">›</span>
                        </div>
                        {roleDetail?.objective && (
                            <p className="role-objective">{roleDetail.objective}</p>
                        )}
                    </div>

                    <h3>📋 단서</h3>
                    <div className="clues-list">
                        {clues.length > 0 ? (
                            clues.map((clue) => (
                                <div
                                    key={clue.id}
                                    className={`clue-card ${getClueTypeClass(clue.clueType)} ${clue.isDiscovered ? 'discovered' : 'locked'}`}
                                >
                                    <span className="clue-icon">
                                        {clue.isDiscovered ? '✅' : '🔒'}
                                    </span>
                                    <span className="clue-type">{getClueTypeLabel(clue.clueType)}</span>
                                    <div className="clue-title">
                                        {clue.isDiscovered ? clue.title : '???'}
                                    </div>
                                    <p className="clue-description">
                                        {clue.isDiscovered ? clue.content : '아직 발견되지 않은 단서입니다.'}
                                    </p>
                                </div>
                            ))
                        ) : (
                            <>
                                <div className="clue-card private">
                                    <span className="clue-icon">🔐</span>
                                    <span className="clue-type">🎭 개인 단서</span>
                                    <div className="clue-title">비밀 정보</div>
                                    <p className="clue-description">
                                        당신만 아는 비밀 정보입니다.
                                    </p>
                                </div>
                                <div className="clue-card public discovered">
                                    <span className="clue-icon">✅</span>
                                    <span className="clue-type">🔍 공개 단서</span>
                                    <div className="clue-title">사건 현장 증거</div>
                                    <p className="clue-description">
                                        사건 현장에서 발견된 증거입니다.
                                    </p>
                                </div>
                            </>
                        )}
                    </div>
                </aside>
            </div>

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

            {/* 모바일 네비게이션 */}
            <MobileNav
                activeTab={mobileTab}
                onTabChange={handleMobileTabChange}
                unreadMessages={unreadMessages}
            />

            {/* Role Modal */}
            {showRoleModal && roleDetail && (
                <div className="modal-overlay" onClick={() => setShowRoleModal(false)}>
                    <div className="modal-content role-modal" onClick={(e) => e.stopPropagation()}>
                        <button className="modal-close" onClick={() => setShowRoleModal(false)}>×</button>
                        <div className="role-header">
                            <span className="role-emoji-lg">{getRoleEmoji(roleDetail.roleType)}</span>
                            <h2>{roleDetail.roleName || roleDetail.roleType}</h2>
                        </div>
                        {roleDetail.description && (
                            <div className="role-section">
                                <h4>📖 설명</h4>
                                <p>{roleDetail.description}</p>
                            </div>
                        )}
                        {roleDetail.objective && (
                            <div className="role-section">
                                <h4>🎯 목표</h4>
                                <p>{roleDetail.objective}</p>
                            </div>
                        )}
                        {roleDetail.secretInfo && (
                            <div className="role-section secret">
                                <h4>🔐 비밀 정보</h4>
                                <p>{roleDetail.secretInfo}</p>
                            </div>
                        )}
                        {roleDetail.alibi && (() => {
                            try {
                                const alibiEntries = JSON.parse(roleDetail.alibi) as { time: string; location: string; activity: string; witnesses?: string[] }[];
                                return (
                                    <div className="role-section alibi">
                                        <h4>📅 사건 당일 알리바이</h4>
                                        <div className="alibi-timeline">
                                            {alibiEntries.map((entry, idx) => (
                                                <div key={idx} className="alibi-entry">
                                                    <span className="alibi-time">{entry.time}</span>
                                                    <div className="alibi-content">
                                                        <span className="alibi-location">📍 {entry.location}</span>
                                                        <span className="alibi-activity">{entry.activity}</span>
                                                        {entry.witnesses && entry.witnesses.length > 0 && (
                                                            <span className="alibi-witnesses">👁️ 목격자: {entry.witnesses.join(', ')}</span>
                                                        )}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                );
                            } catch {
                                return null;
                            }
                        })()}
                    </div>
                </div>
            )}
        </div>
    );
}

