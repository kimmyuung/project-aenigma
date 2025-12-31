import { useEffect, useState } from 'react';
import type { ConnectionState } from '../../hooks/useWebSocket';
import './ConnectionStatus.css';

interface ConnectionStatusProps {
    state: ConnectionState;
    retryCount?: number;
    maxRetries?: number;
    queuedMessageCount?: number;
    onReconnect?: () => void;
}

/**
 * WebSocket 연결 상태를 표시하는 컴포넌트
 * 연결 상태에 따라 적절한 UI를 렌더링합니다.
 */
export function ConnectionStatus({
    state,
    retryCount = 0,
    maxRetries = 10,
    queuedMessageCount = 0,
    onReconnect,
}: ConnectionStatusProps) {
    const [isVisible, setIsVisible] = useState(false);
    const [isExpanded, setIsExpanded] = useState(false);

    // 연결됨 상태가 아닐 때만 표시
    useEffect(() => {
        if (state === 'connected') {
            // 연결되면 잠시 후 숨김
            const timer = setTimeout(() => setIsVisible(false), 2000);
            return () => clearTimeout(timer);
        } else {
            setIsVisible(true);
        }
    }, [state]);

    // 연결됨 상태에서는 짧게만 표시
    if (!isVisible && state === 'connected') {
        return null;
    }

    const statusConfig: Record<ConnectionState, {
        icon: string;
        text: string;
        className: string;
        showRetry: boolean;
        showReconnectButton: boolean;
    }> = {
        connected: {
            icon: '🟢',
            text: '연결됨',
            className: 'connection-status--connected',
            showRetry: false,
            showReconnectButton: false,
        },
        connecting: {
            icon: '🟡',
            text: '연결 중...',
            className: 'connection-status--connecting',
            showRetry: false,
            showReconnectButton: false,
        },
        reconnecting: {
            icon: '🟠',
            text: `재연결 중... (${retryCount}/${maxRetries})`,
            className: 'connection-status--reconnecting',
            showRetry: true,
            showReconnectButton: false,
        },
        disconnected: {
            icon: '🔴',
            text: '연결 끊김',
            className: 'connection-status--disconnected',
            showRetry: false,
            showReconnectButton: true,
        },
        failed: {
            icon: '❌',
            text: '연결 실패',
            className: 'connection-status--failed',
            showRetry: false,
            showReconnectButton: true,
        },
    };

    const config = statusConfig[state];

    return (
        <div
            className={`connection-status ${config.className} ${isExpanded ? 'connection-status--expanded' : ''}`}
            onClick={() => setIsExpanded(!isExpanded)}
        >
            <div className="connection-status__main">
                <span className="connection-status__icon">{config.icon}</span>
                <span className="connection-status__text">{config.text}</span>

                {/* 대기 중인 메시지 표시 */}
                {queuedMessageCount > 0 && (
                    <span className="connection-status__queue">
                        📨 {queuedMessageCount}개 대기
                    </span>
                )}
            </div>

            {/* 확장 시 상세 정보 */}
            {isExpanded && (
                <div className="connection-status__details">
                    {state === 'reconnecting' && (
                        <p className="connection-status__info">
                            네트워크 연결을 복구하는 중입니다. 잠시만 기다려주세요.
                        </p>
                    )}

                    {state === 'disconnected' && (
                        <p className="connection-status__info">
                            네트워크 연결이 끊어졌습니다. 네트워크 상태를 확인해주세요.
                        </p>
                    )}

                    {state === 'failed' && (
                        <p className="connection-status__info">
                            연결에 실패했습니다. 네트워크를 확인하고 다시 시도해주세요.
                        </p>
                    )}

                    {queuedMessageCount > 0 && (
                        <p className="connection-status__queue-info">
                            연결이 복구되면 {queuedMessageCount}개의 메시지가 자동으로 전송됩니다.
                        </p>
                    )}

                    {config.showReconnectButton && onReconnect && (
                        <button
                            className="connection-status__reconnect-btn"
                            onClick={(e) => {
                                e.stopPropagation();
                                onReconnect();
                            }}
                        >
                            🔄 다시 연결
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}

export default ConnectionStatus;
