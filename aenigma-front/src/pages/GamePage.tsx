import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { gameApi, type GamePlayer } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { useWebSocket, type ChatMessage } from '../hooks/useWebSocket';
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

    // WebSocket 메시지 수신 핸들러
    const handleWebSocketMessage = useCallback((message: ChatMessage) => {
        setMessages(prev => [...prev, message]);
    }, []);

    // WebSocket 연결
    const {
        isConnected,
        sendPublicMessage,
        sendWhisper
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

    const handleVote = () => {
        if (selectedPlayer) {
            setVotedPlayer(selectedPlayer);
            // TODO: 투표 API 호출
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

    if (isLoading || !game) {
        return (
            <div className="game-page">
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>게임을 불러오는 중...</p>
                </div>
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
                <div className="my-role">
                    <span className="role-emoji">{getRoleEmoji(game.myRole)}</span>
                    <span className="role-name">{game.myRole || '역할 미정'}</span>
                </div>
            </header>

            <div className="game-content">
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
                            <div className="vote-actions">
                                <button
                                    className="btn btn-primary btn-lg"
                                    onClick={handleVote}
                                    disabled={!selectedPlayer || !!votedPlayer}
                                >
                                    {votedPlayer ? '투표 완료' : '투표하기'}
                                </button>
                            </div>
                        </div>
                    )}
                </main>

                {/* Clues Panel */}
                <aside className="clues-panel">
                    <h3>📋 단서</h3>
                    <div className="clues-list">
                        <div className="clue-item private">
                            <span className="clue-type">개인 단서</span>
                            <p>당신만 아는 비밀 정보입니다.</p>
                        </div>
                        <div className="clue-item public">
                            <span className="clue-type">공개 단서</span>
                            <p>사건 현장에서 발견된 증거입니다.</p>
                        </div>
                    </div>
                </aside>
            </div>
        </div>
    );
}
