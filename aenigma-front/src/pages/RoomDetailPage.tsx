import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { roomApi, type Room } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { Header } from '../components/Header';
import './RoomDetailPage.css';

interface RoomMember {
    id: string;
    nickname: string;
    isHost: boolean;
    isReady: boolean;
}

export function RoomDetailPage() {
    const { roomId } = useParams<{ roomId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [room, setRoom] = useState<Room | null>(null);
    const [members, setMembers] = useState<RoomMember[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isHost, setIsHost] = useState(false);

    useEffect(() => {
        if (roomId) {
            loadRoomDetail();
        }
    }, [roomId]);

    const loadRoomDetail = async () => {
        try {
            setIsLoading(true);
            const response = await roomApi.get(roomId!);
            setRoom(response.data);
            // 임시 멤버 데이터 (실제로는 API에서 받아옴)
            setMembers([
                { id: '1', nickname: response.data.hostNickname, isHost: true, isReady: true },
            ]);
            setIsHost(response.data.hostNickname === user?.nickname);
        } catch (err) {
            setError('방 정보를 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleStartGame = async () => {
        try {
            // 게임 시작 API 호출
            navigate(`/game/${roomId}`);
        } catch (err) {
            alert('게임 시작에 실패했습니다.');
        }
    };

    const handleLeaveRoom = async () => {
        try {
            await roomApi.leave(roomId!);
            navigate('/rooms');
        } catch (err) {
            alert('방 퇴장에 실패했습니다.');
        }
    };

    const copyRoomCode = () => {
        if (room?.roomCode) {
            navigator.clipboard.writeText(room.roomCode);
            alert('방 코드가 복사되었습니다!');
        }
    };

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
                    </div>
                </div>

                <div className="room-content">
                    <div className="members-section">
                        <h2>참가자 목록</h2>
                        <div className="members-grid">
                            {members.map((member) => (
                                <div key={member.id} className={`member-card ${member.isHost ? 'host' : ''}`}>
                                    <div className="member-avatar">
                                        {member.nickname.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="member-info">
                                        <span className="member-name">{member.nickname}</span>
                                        {member.isHost && <span className="host-badge">👑 방장</span>}
                                    </div>
                                    <div className={`ready-status ${member.isReady ? 'ready' : ''}`}>
                                        {member.isReady ? '✅' : '⏳'}
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

                    <div className="game-settings">
                        <h2>게임 설정</h2>
                        <div className="settings-list">
                            <div className="setting-item">
                                <span className="setting-label">최대 인원</span>
                                <span className="setting-value">{room.maxPlayers}명</span>
                            </div>
                            <div className="setting-item">
                                <span className="setting-label">조사 라운드</span>
                                <span className="setting-value">3회</span>
                            </div>
                            <div className="setting-item">
                                <span className="setting-label">범인 수</span>
                                <span className="setting-value">1명</span>
                            </div>
                        </div>
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
                            disabled={members.length < 4}
                        >
                            🎮 게임 시작
                        </button>
                    ) : (
                        <button className="btn btn-primary btn-lg">
                            ✅ 준비 완료
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
}
