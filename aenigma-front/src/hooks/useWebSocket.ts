import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8081/ws';

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

interface UseWebSocketOptions {
    gameId: string;
    userId: string;
    onMessage?: (message: ChatMessage) => void;
    onConnect?: () => void;
    onDisconnect?: () => void;
    onError?: (error: string) => void;
}

export function useWebSocket({
    gameId,
    userId,
    onMessage,
    onConnect,
    onDisconnect,
    onError,
}: UseWebSocketOptions) {
    const clientRef = useRef<Client | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [connectionError, setConnectionError] = useState<string | null>(null);

    // 연결
    useEffect(() => {
        if (!gameId || !userId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            connectHeaders: {
                'X-User-Id': userId,
            },
            debug: (str) => {
                console.log('[STOMP]', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log('WebSocket 연결됨');
            setIsConnected(true);
            setConnectionError(null);
            onConnect?.();

            // 게임 채팅 구독
            client.subscribe(`/topic/game/${gameId}`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('메시지 파싱 실패', e);
                }
            });

            // 개인 메시지 (귓속말) 구독
            client.subscribe(`/user/${userId}/queue/whisper`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('귓속말 파싱 실패', e);
                }
            });

            // 시스템 메시지 구독
            client.subscribe(`/topic/game/${gameId}/system`, (message: IMessage) => {
                try {
                    const chatMessage: ChatMessage = JSON.parse(message.body);
                    onMessage?.(chatMessage);
                } catch (e) {
                    console.error('시스템 메시지 파싱 실패', e);
                }
            });
        };

        client.onDisconnect = () => {
            console.log('WebSocket 연결 해제됨');
            setIsConnected(false);
            onDisconnect?.();
        };

        client.onStompError = (frame) => {
            console.error('STOMP 에러', frame);
            setConnectionError(frame.headers['message'] || 'WebSocket 연결 에러');
            onError?.(frame.headers['message'] || 'WebSocket 연결 에러');
        };

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
            clientRef.current = null;
        };
    }, [gameId, userId]);

    // 메시지 전송
    const sendMessage = useCallback((content: string, type: 'PUBLIC' | 'WHISPER' = 'PUBLIC', receiverId?: string) => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket 연결되지 않음');
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
    }, [gameId, userId]);

    // 공개 채팅
    const sendPublicMessage = useCallback((content: string) => {
        return sendMessage(content, 'PUBLIC');
    }, [sendMessage]);

    // 귓속말
    const sendWhisper = useCallback((content: string, receiverId: string) => {
        return sendMessage(content, 'WHISPER', receiverId);
    }, [sendMessage]);

    return {
        isConnected,
        connectionError,
        sendMessage,
        sendPublicMessage,
        sendWhisper,
    };
}
