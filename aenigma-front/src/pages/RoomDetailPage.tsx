import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { roomApi, type Room } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { Header } from '../components/Header';
import { useRoomWebSocket, type RoomMessage, type RoomEvent } from '../hooks/useRoomWebSocket';
import './RoomDetailPage.css';

export function RoomDetailPage() {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();
    const chatContainerRef = useRef<HTMLDivElement>(null);

    const [room, setRoom] = useState<Room | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isHost, setIsHost] = useState(false);
    const [isReady, setIsReady] = useState(false);

    // 채팅 상태
    const [messages, setMessages] = useState<RoomMessage[]>([]);
    const [newMessage, setNewMessage] = useState('');

    // WebSocket 연결
    const handleMessage = useCallback((message: RoomMessage) => {
        setMessages(prev => [...prev, message]);
    }, []);

    const handleRoomEvent = useCallback((event: RoomEvent) => {
        if (event.type === 'GAME_STARTED' && event.gameId) {
            navigate(`/game/${event.gameId}`);
        } else if (event.type === 'ROOM_CLOSED') {
            alert('방이 닫혔습니다.');
            navigate('/rooms');
        }
    }, [navigate]);

    const {
        isConnected,
        members,
        sendMessage,
        toggleReady,
        startGame,
    } = useRoomWebSocket({
        roomId: roomId || '',
        userId: user?.userId || '',
        onMessage: handleMessage,
        onRoomEvent: handleRoomEvent,
        onConnect: () => {
            setMessages(prev => [...prev, {
                id: `sys-${Date.now()}`,
                roomId: roomId!,
                senderId: 'system',
                senderNickname: '시스템',
                content: '대기실에 입장하셨습니다.',
                type: 'SYSTEM',
                timestamp: new Date().toISOString(),
            }]);
        },
    });

    useEffect(() => {
        if (roomId) {
            loadRoomDetail();
        }
    }, [roomId]);

    useEffect(() => {
        // 채팅 스크롤 자동 하단
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [messages]);

    // 내 준비 상태 업데이트
    useEffect(() => {
        const myMember = members.find(m => m.nickname === user?.nickname);
        if (myMember) {
            setIsReady(myMember.isReady);
            setIsHost(myMember.isHost);
        }
    }, [members, user?.nickname]);

    const loadRoomDetail = async () => {
        try {
            setIsLoading(true);
            const response = await roomApi.get(roomId!);
            setRoom(response.data);
            setIsHost(response.data.hostNickname === user?.nickname);
        } catch (err) {
            setError('방 정보를 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleStartGame = () => {
        startGame();
    };

    const handleLeaveRoom = async () => {
        try {
            await roomApi.leave(roomId!);
            navigate('/rooms');
        } catch (err) {
            alert('방 퇴장에 실패했습니다.');
        }
    };

    const handleToggleReady = () => {
        toggleReady();
    };

    const handleSendMessage = (e: React.FormEvent) => {
        e.preventDefault();
        if (!newMessage.trim()) return;

        sendMessage(newMessage);
        // 낙관적 UI 업데이트
        setMessages(prev => [...prev, {
            id: `temp-${Date.now()}`,
            roomId: roomId!,
            senderId: user?.userId || '',
            senderNickname: user?.nickname || '나',
            content: newMessage,
            type: 'CHAT',
            timestamp: new Date().toISOString(),
        }]);
        setNewMessage('');
    };

    const copyRoomCode = () => {
        if (room?.roomCode) {
            navigator.clipboard.writeText(room.roomCode);
            alert('방 코드가 복사되었습니다!');
        }
    };

    // 모든 참가자 준비 완료 확인
    const allReady = members.length >= 2 && members.every(m => m.isReady || m.isHost);

    if (isLoading) {
        return (
            <div className="room-detail-page">
                <Header />
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>방 정보를 불러오는 중...</p>
                </div>
            </div>
        );
    }

    if (error || !room) {
        return (
            <div className="room-detail-page">
                <Header />
                <div className="error-container">
                    <p>{error || '방을 찾을 수 없습니다.'}</p>
                    <button className="btn btn-primary" onClick={() => navigate('/rooms')}>
                        방 목록으로
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="room-detail-page">
            <Header />

            <main className="room-detail-main">
                <div className="room-detail-header">
                    <div className="room-title-section">
                        <h1>{room.title}</h1>
                        <div className="room-code-badge" onClick={copyRoomCode}>
                            <span className="code-label">방 코드</span>
                            <code className="code-value">{room.roomCode}</code>
                            <span className="copy-icon">📋</span>
                        </div>
                    </div>
                    <div className="room-info-badges">
                        <span className="badge badge-info">
                            👥 {members.length} / {room.maxPlayers}
                        </span>
                        <span className={`badge ${room.isPrivate ? 'badge-warning' : 'badge-success'}`}>
                            {room.isPrivate ? '🔐 비공개' : '🌐 공개'}
                        </span>
                        <span className={`badge ${isConnected ? 'badge-success' : 'badge-error'}`}>
                            {isConnected ? '🟢 연결됨' : '🔴 연결 끊김'}
                        </span>
                    </div>
                </div>

                <div className="room-content">
                    <div className="members-section">
                        <h2>참가자 목록</h2>
                        <div className="members-grid">
                            {members.map((member) => (
                                <div key={member.id} className={`member-card ${member.isHost ? 'host' : ''} ${member.isReady ? 'ready' : ''}`}>
                                    <div className="member-avatar">
                                        {member.nickname.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="member-info">
                                        <span className="member-name">{member.nickname}</span>
                                        {member.isHost && <span className="host-badge">👑 방장</span>}
                                    </div>
                                    <div className={`ready-status ${member.isReady || member.isHost ? 'ready' : ''}`}>
                                        {member.isHost ? '👑' : member.isReady ? '✅' : '⏳'}
                                    </div>
                                </div>
                            ))}

                            {/* 빈 슬롯 */}
                            {Array.from({ length: room.maxPlayers - members.length }).map((_, i) => (
                                <div key={`empty-${i}`} className="member-card empty">
                                    <div className="member-avatar empty">?</div>
                                    <div className="member-info">
                                        <span className="member-name">대기 중...</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* 대기실 채팅 */}
                    <div className="chat-section">
                        <h2>💬 대기실 채팅</h2>
                        <div className="chat-container" ref={chatContainerRef}>
                            {messages.map((msg) => (
                                <div
                                    key={msg.id}
                                    className={`chat-message ${msg.type.toLowerCase()} ${msg.senderId === user?.userId ? 'mine' : ''}`}
                                >
                                    {msg.type === 'SYSTEM' || msg.type === 'JOIN' || msg.type === 'LEAVE' ? (
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
                                disabled={!isConnected}
                            />
                            <button type="submit" className="btn btn-primary" disabled={!isConnected}>
                                전송
                            </button>
                        </form>
                    </div>
                </div>

                <div className="room-actions">
                    <button className="btn btn-secondary" onClick={handleLeaveRoom}>
                        🚪 방 나가기
                    </button>
                    {isHost ? (
                        <button
                            className="btn btn-primary btn-lg"
                            onClick={handleStartGame}
                            disabled={!allReady || members.length < 2}
                            title={!allReady ? '모든 참가자가 준비해야 합니다' : members.length < 2 ? '최소 2명이 필요합니다' : ''}
                        >
                            🎮 게임 시작 {!allReady && `(${members.filter(m => m.isReady || m.isHost).length}/${members.length} 준비)`}
                        </button>
                    ) : (
                        <button
                            className={`btn ${isReady ? 'btn-secondary' : 'btn-primary'} btn-lg`}
                            onClick={handleToggleReady}
                        >
                            {isReady ? '❌ 준비 취소' : '✅ 준비 완료'}
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
}
