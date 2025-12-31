import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8081/ws';

// 재연결 설정
const RECONNECT_CONFIG = {
    initialDelay: 1000,        // 첫 재연결: 1초
    maxDelay: 30000,           // 최대 대기: 30초
    maxRetries: 10,            // 최대 10회 시도
    multiplier: 1.5,           // 지수 배율
};

export interface ChatMessage {
    id: string;
    gameId: string;
    senderId: string;
    senderNickname: string;
    content: string;
    type: 'PUBLIC' | 'WHISPER' | 'SYSTEM';
    receiverId?: string;
    timestamp: string;
}

// 연결 상태 타입
export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'failed';

// 대기 중인 메시지
interface QueuedMessage {
    content: string;
    type: 'PUBLIC' | 'WHISPER';
    receiverId?: string;
    timestamp: number;
}

interface UseWebSocketOptions {
    gameId: string;
    userId: string;
    onMessage?: (message: ChatMessage) => void;
    onConnect?: () => void;
    onDisconnect?: () => void;
    onError?: (error: string) => void;
    onConnectionStateChange?: (state: ConnectionState) => void;
}

export function useWebSocket({
    gameId,
    userId,
    onMessage,
    onConnect,
    onDisconnect,
    onError,
    onConnectionStateChange,
}: UseWebSocketOptions) {
    const clientRef = useRef<Client | null>(null);
    const retryCountRef = useRef(0);
    const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const messageQueueRef = useRef<QueuedMessage[]>([]);

    const [isConnected, setIsConnected] = useState(false);
    const [connectionState, setConnectionState] = useState<ConnectionState>('disconnected');
    const [connectionError, setConnectionError] = useState<string | null>(null);
    const [retryCount, setRetryCount] = useState(0);

    // 연결 상태 변경 시 콜백 호출
    const updateConnectionState = useCallback((newState: ConnectionState) => {
        setConnectionState(newState);
        onConnectionStateChange?.(newState);
    }, [onConnectionStateChange]);

    // 지수 백오프 재연결 딜레이 계산
    const getReconnectDelay = useCallback((attempt: number): number => {
        const delay = RECONNECT_CONFIG.initialDelay *
            Math.pow(RECONNECT_CONFIG.multiplier, attempt);
        return Math.min(delay, RECONNECT_CONFIG.maxDelay);
    }, []);

    // 메시지 큐 비우기 (재연결 시 호출)
    const flushMessageQueue = useCallback(() => {
        const queue = [...messageQueueRef.current];
        messageQueueRef.current = [];

        if (queue.length > 0 && clientRef.current?.connected) {
            console.log(`[WebSocket] ${queue.length}개의 대기 메시지 전송 중...`);
            queue.forEach(msg => {
                clientRef.current?.publish({
                    destination: '/app/chat/message',
                    body: JSON.stringify({
                        gameId,
                        senderId: userId,
                        content: msg.content,
                        type: msg.type,
                        receiverId: msg.receiverId,
                    }),
                });
            });

            // 사용자에게 알림 (커스텀 이벤트)
            window.dispatchEvent(new CustomEvent('websocket-queue-flushed', {
                detail: { count: queue.length }
            }));
        }
    }, [gameId, userId]);

    // 재연결 시도
    const attemptReconnect = useCallback(() => {
        if (retryCountRef.current >= RECONNECT_CONFIG.maxRetries) {
            console.error('[WebSocket] 최대 재연결 시도 횟수 초과');
            updateConnectionState('failed');
            setConnectionError('연결에 실패했습니다. 페이지를 새로고침해주세요.');
            onError?.('최대 재연결 시도 횟수를 초과했습니다.');
            return;
        }

        const delay = getReconnectDelay(retryCountRef.current);
        console.log(`[WebSocket] ${delay}ms 후 재연결 시도 (${retryCountRef.current + 1}/${RECONNECT_CONFIG.maxRetries})`);

        updateConnectionState('reconnecting');
        setRetryCount(retryCountRef.current);

        reconnectTimeoutRef.current = setTimeout(() => {
            retryCountRef.current++;
            setRetryCount(retryCountRef.current);
            clientRef.current?.activate();
        }, delay);
    }, [getReconnectDelay, updateConnectionState, onError]);

    // 연결 정리
    const cleanup = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
            reconnectTimeoutRef.current = null;
        }
        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
        }
    }, []);

    // 수동 재연결
    const reconnect = useCallback(() => {
        retryCountRef.current = 0;
        setRetryCount(0);
        setConnectionError(null);
        updateConnectionState('connecting');

        cleanup();
        // 새 클라이언트 생성은 useEffect에서 처리
    }, [cleanup, updateConnectionState]);

    // 메인 연결 Effect
    useEffect(() => {
        if (!gameId || !userId) return;

        updateConnectionState('connecting');

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            connectHeaders: {
                'X-User-Id': userId,
            },
            debug: (str) => {
                if (import.meta.env.DEV) {
                    console.log('[STOMP]', str);
                }
            },
            // 재연결은 수동으로 관리
            reconnectDelay: 0,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log('[WebSocket] 연결됨');
            retryCountRef.current = 0;
            setRetryCount(0);
            setIsConnected(true);
            setConnectionError(null);
            updateConnectionState('connected');
            onConnect?.();

            // 대기 중인 메시지 전송
            flushMessageQueue();

            // 게임 채팅 구독
            client.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('[WebSocket] 메시지 파싱 실패', e);
                }
            });

            // 개인 메시지 (귓속말) 구독
            client.subscribe(`/user/${userId}/queue/whisper`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('[WebSocket] 귓속말 파싱 실패', e);
                }
            });

            // 시스템 메시지 구독
            client.subscribe(`/topic/game/${gameId}/system`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('[WebSocket] 시스템 메시지 파싱 실패', e);
                }
            });
        };

        client.onDisconnect = () => {
            console.log('[WebSocket] 연결 해제됨');
            setIsConnected(false);
            onDisconnect?.();

            // 네트워크가 온라인 상태라면 재연결 시도
            if (navigator.onLine) {
                attemptReconnect();
            } else {
                updateConnectionState('disconnected');
            }
        };

        client.onStompError = (frame) => {
            console.error('[WebSocket] STOMP 에러', frame);
            const errorMessage = frame.headers['message'] || 'WebSocket 연결 에러';
            setConnectionError(errorMessage);
            onError?.(errorMessage);

            // 에러 이벤트 발생 (토스트 표시용)
            window.dispatchEvent(new CustomEvent('websocket-error', {
                detail: { message: errorMessage }
            }));

            // 재연결 시도
            attemptReconnect();
        };

        client.onWebSocketError = (event) => {
            console.error('[WebSocket] WebSocket 에러', event);
            // 재연결 시도는 onDisconnect에서 처리
        };

        client.activate();
        clientRef.current = client;

        // 네트워크 상태 감지
        const handleOnline = () => {
            console.log('[WebSocket] 네트워크 온라인 감지');
            if (connectionState === 'disconnected' || connectionState === 'failed') {
                retryCountRef.current = 0;
                setRetryCount(0);
                attemptReconnect();
            }
        };

        const handleOffline = () => {
            console.log('[WebSocket] 네트워크 오프라인 감지');
            updateConnectionState('disconnected');
            // 재연결 타이머 취소
            if (reconnectTimeoutRef.current) {
                clearTimeout(reconnectTimeoutRef.current);
                reconnectTimeoutRef.current = null;
            }
        };

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
            cleanup();
        };
    }, [gameId, userId, onMessage, onConnect, onDisconnect, onError,
        attemptReconnect, flushMessageQueue, updateConnectionState, cleanup, connectionState]);

    // 메시지 전송
    const sendMessage = useCallback((
        content: string,
        type: 'PUBLIC' | 'WHISPER' = 'PUBLIC',
        receiverId?: string
    ): boolean => {
        // 연결되지 않은 경우
        if (!clientRef.current?.connected) {
            // 재연결 중이면 큐에 저장
            if (connectionState === 'reconnecting' || connectionState === 'connecting') {
                messageQueueRef.current.push({
                    content,
                    type,
                    receiverId,
                    timestamp: Date.now()
                });
                console.log('[WebSocket] 메시지 큐에 추가됨 (대기 중)');

                // 사용자 알림 이벤트
                window.dispatchEvent(new CustomEvent('websocket-message-queued', {
                    detail: { queueLength: messageQueueRef.current.length }
                }));
                return true;
            }

            console.error('[WebSocket] 연결되지 않음');
            window.dispatchEvent(new CustomEvent('websocket-send-failed', {
                detail: { reason: '연결되지 않음' }
            }));
            return false;
        }

        const message = {
            gameId,
            senderId: userId,
            content,
            type,
            receiverId,
        };

        clientRef.current.publish({
            destination: '/app/chat/message',
            body: JSON.stringify(message),
        });

        return true;
    }, [gameId, userId, connectionState]);

    // 공개 채팅
    const sendPublicMessage = useCallback((content: string) => {
        return sendMessage(content, 'PUBLIC');
    }, [sendMessage]);

    // 귓속말
    const sendWhisper = useCallback((content: string, receiverId: string) => {
        return sendMessage(content, 'WHISPER', receiverId);
    }, [sendMessage]);

    return {
        // 상태
        isConnected,
        connectionState,
        connectionError,
        retryCount,
        maxRetries: RECONNECT_CONFIG.maxRetries,
        queuedMessageCount: messageQueueRef.current.length,

        // 액션
        sendMessage,
        sendPublicMessage,
        sendWhisper,
        reconnect,
    };
}
