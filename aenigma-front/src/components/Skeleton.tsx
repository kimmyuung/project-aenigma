import './Skeleton.css';

interface SkeletonProps {
    variant?: 'text' | 'title' | 'avatar' | 'card' | 'button';
    width?: string;
    height?: string;
    className?: string;
    count?: number;
}

export function Skeleton({
    variant = 'text',
    width,
    height,
    className = '',
    count = 1,
}: SkeletonProps) {
    const getVariantClass = () => {
        switch (variant) {
            case 'title':
                return 'skeleton-title';
            case 'avatar':
                return 'skeleton-avatar';
            case 'card':
                return 'skeleton-card';
            case 'button':
                return 'skeleton-button';
            default:
                return 'skeleton-text';
        }
    };

    const style = {
        width: width || undefined,
        height: height || undefined,
    };

    if (count > 1) {
        return (
            <div className="skeleton-group">
                {Array.from({ length: count }).map((_, i) => (
                    <div
                        key={i}
                        className={`skeleton ${getVariantClass()} ${className}`}
                        style={style}
                    />
                ))}
            </div>
        );
    }

    return (
        <div
            className={`skeleton ${getVariantClass()} ${className}`}
            style={style}
        />
    );
}

// 플레이어 목록용 스켈레톤
export function PlayerListSkeleton({ count = 5 }: { count?: number }) {
    return (
        <div className="skeleton-player-list">
            {Array.from({ length: count }).map((_, i) => (
                <div key={i} className="skeleton-player-item">
                    <Skeleton variant="avatar" />
                    <div className="skeleton-player-info">
                        <Skeleton variant="text" width="80%" />
                        <Skeleton variant="text" width="50%" height="0.75rem" />
                    </div>
                </div>
            ))}
        </div>
    );
}

// 단서 목록용 스켈레톤
export function ClueListSkeleton({ count = 3 }: { count?: number }) {
    return (
        <div className="skeleton-clue-list">
            {Array.from({ length: count }).map((_, i) => (
                <div key={i} className="skeleton-clue-card">
                    <Skeleton variant="text" width="40%" height="0.75rem" />
                    <Skeleton variant="title" />
                    <Skeleton variant="text" count={2} />
                </div>
            ))}
        </div>
    );
}

// 방 카드용 스켈레톤
export function RoomCardSkeleton({ count = 4 }: { count?: number }) {
    return (
        <div className="skeleton-rooms-grid">
            {Array.from({ length: count }).map((_, i) => (
                <div key={i} className="skeleton-room-card">
                    <div className="skeleton-room-header">
                        <Skeleton variant="title" width="70%" />
                        <Skeleton variant="text" width="60px" height="24px" />
                    </div>
                    <div className="skeleton-room-info">
                        <Skeleton variant="text" width="100%" />
                        <Skeleton variant="text" width="80%" />
                    </div>
                    <Skeleton variant="button" width="100%" height="40px" />
                </div>
            ))}
        </div>
    );
}

// 채팅 메시지용 스켈레톤
export function ChatSkeleton({ count = 5 }: { count?: number }) {
    return (
        <div className="skeleton-chat">
            {Array.from({ length: count }).map((_, i) => (
                <div
                    key={i}
                    className={`skeleton-chat-message ${i % 3 === 0 ? 'mine' : ''}`}
                >
                    <Skeleton variant="text" width={`${60 + Math.random() * 30}%`} />
                </div>
            ))}
        </div>
    );
}
