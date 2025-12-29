import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { roomApi, type Room } from '../api/client';
import { useAuth } from '../contexts/AuthContext';
import { Header } from '../components/Header';
import { RoomCardSkeleton } from '../components/Skeleton';
import './RoomsPage.css';

export function RoomsPage() {
    const [rooms, setRooms] = useState<Room[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showJoinModal, setShowJoinModal] = useState(false);
    const { isAuthenticated } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login');
            return;
        }
        loadRooms();
    }, [isAuthenticated, navigate]);

    const loadRooms = async () => {
        try {
            setIsLoading(true);
            const response = await roomApi.joinable();
            setRooms(response.data);
        } catch (err) {
            setError('방 목록을 불러오는데 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };


    return (
        <div className="rooms-page">
            <Header />

            <main className="rooms-main">
                <div className="rooms-header">
                    <div>
                        <h1>게임 방 목록</h1>
                        <p className="text-muted">입장 가능한 방을 선택하거나 새 방을 만드세요</p>
                    </div>
                    <div className="rooms-actions">
                        <button
                            className="btn btn-secondary"
                            onClick={() => setShowJoinModal(true)}
                        >
                            🔑 코드로 입장
                        </button>
                        <button
                            className="btn btn-primary"
                            onClick={() => setShowCreateModal(true)}
                        >
                            ➕ 방 만들기
                        </button>
                    </div>
                </div>

                {isLoading ? (
                    <RoomCardSkeleton count={4} />
                ) : error ? (
                    <div className="rooms-error">
                        <p>{error}</p>
                        <button className="btn btn-secondary" onClick={loadRooms}>
                            다시 시도
                        </button>
                    </div>
                ) : rooms.length === 0 ? (
                    <div className="rooms-empty">
                        <span className="empty-icon">🎭</span>
                        <h3>입장 가능한 방이 없습니다</h3>
                        <p>새로운 방을 만들어 게임을 시작하세요!</p>
                        <button
                            className="btn btn-primary btn-lg"
                            onClick={() => setShowCreateModal(true)}
                        >
                            방 만들기
                        </button>
                    </div>
                ) : (
                    <div className="rooms-grid">
                        {rooms.map((room) => (
                            <RoomCard key={room.id} room={room} onJoined={loadRooms} />
                        ))}
                    </div>
                )}
            </main>

            {showCreateModal && (
                <CreateRoomModal
                    onClose={() => setShowCreateModal(false)}
                    onCreated={() => { setShowCreateModal(false); loadRooms(); }}
                />
            )}

            {showJoinModal && (
                <JoinRoomModal
                    onClose={() => setShowJoinModal(false)}
                    onJoined={() => { setShowJoinModal(false); loadRooms(); }}
                />
            )}
        </div>
    );
}

function RoomCard({ room, onJoined: _onJoined }: { room: Room; onJoined: () => void }) {
    const [isJoining, setIsJoining] = useState(false);
    const navigate = useNavigate();

    const handleJoin = async () => {
        setIsJoining(true);
        try {
            await roomApi.join({ roomCode: room.roomCode });
            navigate(`/room/${room.id}`);
        } catch {
            alert('방 입장에 실패했습니다.');
        } finally {
            setIsJoining(false);
        }
    };

    return (
        <div className="room-card card">
            <div className="room-card-header">
                <h3 className="room-title">{room.title}</h3>
                {getStatusBadge(room.status)}
            </div>
            <div className="room-info">
                <div className="room-info-item">
                    <span className="info-label">👤 방장</span>
                    <span className="info-value">{room.hostNickname}</span>
                </div>
                <div className="room-info-item">
                    <span className="info-label">👥 인원</span>
                    <span className="info-value">{room.currentPlayerCount} / {room.maxPlayers}</span>
                </div>
                <div className="room-info-item">
                    <span className="info-label">🔐 유형</span>
                    <span className="info-value">{room.isPrivate ? '비공개' : '공개'}</span>
                </div>
            </div>
            <div className="room-code">
                <span className="code-label">방 코드</span>
                <code className="code-value font-mono">{room.roomCode}</code>
            </div>
            <button
                className="btn btn-primary btn-block"
                onClick={handleJoin}
                disabled={isJoining || room.currentPlayerCount >= room.maxPlayers}
            >
                {isJoining ? '입장 중...' : room.currentPlayerCount >= room.maxPlayers ? '인원 초과' : '입장하기'}
            </button>
        </div>
    );
}

function getStatusBadge(status: Room['status']) {
    switch (status) {
        case 'WAITING':
            return <span className="badge badge-success">대기중</span>;
        case 'PLAYING':
            return <span className="badge badge-warning">진행중</span>;
        case 'FINISHED':
            return <span className="badge">종료됨</span>;
        default:
            return null;
    }
}

function CreateRoomModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
    const [title, setTitle] = useState('');
    const [maxPlayers, setMaxPlayers] = useState(6);
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        setIsLoading(true);
        try {
            await roomApi.create({
                title,
                maxPlayers,
                password: password || undefined
            });
            onCreated();
        } catch {
            alert('방 생성에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>새 방 만들기</h2>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <form onSubmit={handleSubmit} className="modal-body">
                    <div className="input-group">
                        <label className="input-label">방 제목</label>
                        <input
                            type="text"
                            className="input"
                            placeholder="예: 즐거운 추리 게임"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            maxLength={50}
                            autoFocus
                        />
                    </div>
                    <div className="input-group">
                        <label className="input-label">최대 인원</label>
                        <select
                            className="input"
                            value={maxPlayers}
                            onChange={(e) => setMaxPlayers(Number(e.target.value))}
                        >
                            {[4, 5, 6, 7, 8].map((n) => (
                                <option key={n} value={n}>{n}명</option>
                            ))}
                        </select>
                    </div>
                    <div className="input-group">
                        <label className="input-label">비밀번호 (선택)</label>
                        <input
                            type="password"
                            className="input"
                            placeholder="비공개 방으로 설정"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <div className="modal-actions">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>
                            취소
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={isLoading}>
                            {isLoading ? '생성 중...' : '방 만들기'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

function JoinRoomModal({ onClose, onJoined }: { onClose: () => void; onJoined: () => void }) {
    const [roomCode, setRoomCode] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!roomCode.trim()) return;

        setIsLoading(true);
        try {
            const response = await roomApi.join({
                roomCode: roomCode.toUpperCase(),
                password: password || undefined
            });
            navigate(`/room/${response.data.id}`);
            onJoined();
        } catch {
            alert('방 입장에 실패했습니다. 코드를 확인해주세요.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>코드로 입장</h2>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <form onSubmit={handleSubmit} className="modal-body">
                    <div className="input-group">
                        <label className="input-label">방 코드</label>
                        <input
                            type="text"
                            className="input font-mono"
                            placeholder="ABC123"
                            value={roomCode}
                            onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
                            maxLength={10}
                            autoFocus
                        />
                    </div>
                    <div className="input-group">
                        <label className="input-label">비밀번호 (필요시)</label>
                        <input
                            type="password"
                            className="input"
                            placeholder="비공개 방인 경우"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <div className="modal-actions">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>
                            취소
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={isLoading}>
                            {isLoading ? '입장 중...' : '입장하기'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
