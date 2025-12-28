import { useEffect, useRef, useState, useCallback } from 'react';
import { tokenStorage } from './api';

const WS_BASE_URL = process.env.EXPO_PUBLIC_WS_URL || 'ws://localhost:8080';

export interface ChatMessage {
    id: string;
    gameId: string;
    senderId: string;
    senderNickname: string;
    content: string;
    type: 'PUBLIC' | 'WHISPER' | 'SYSTEM' | 'GM';
    timestamp: string;
}

interface UseWebSocketOptions {
    gameId: string;
    userId: string;
    onMessage: (message: ChatMessage) => void;
    onConnect?: () => void;
    onDisconnect?: () => void;
}

export function useWebSocket({
    gameId,
    userId,
    onMessage,
    onConnect,
    onDisconnect,
}: UseWebSocketOptions) {
    const wsRef = useRef<WebSocket | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const reconnectAttempts = useRef(0);
    const maxReconnectAttempts = 5;

    const connect = useCallback(async () => {
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            return;
        }

        try {
            const token = await tokenStorage.getAccessToken();
            const wsUrl = `${WS_BASE_URL}/ws/chat?gameId=${gameId}&userId=${userId}&token=${token || ''}`;

            const ws = new WebSocket(wsUrl);

            ws.onopen = () => {
                console.log('WebSocket connected');
                setIsConnected(true);
                reconnectAttempts.current = 0;
                onConnect?.();
            };

            ws.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data) as ChatMessage;
                    onMessage(message);
                } catch (error) {
                    console.error('Failed to parse message', error);
                }
            };

            ws.onclose = () => {
                console.log('WebSocket disconnected');
                setIsConnected(false);
                onDisconnect?.();

                // 자동 재연결
                if (reconnectAttempts.current < maxReconnectAttempts) {
                    reconnectAttempts.current += 1;
                    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts.current), 30000);
                    reconnectTimeoutRef.current = setTimeout(connect, delay);
                }
            };

            ws.onerror = (error) => {
                console.error('WebSocket error', error);
            };

            wsRef.current = ws;
        } catch (error) {
            console.error('Failed to connect WebSocket', error);
        }
    }, [gameId, userId, onMessage, onConnect, onDisconnect]);

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
        setIsConnected(false);
    }, []);

    const sendMessage = useCallback((content: string, type: 'PUBLIC' | 'WHISPER' = 'PUBLIC', targetId?: string) => {
        if (wsRef.current?.readyState !== WebSocket.OPEN) {
            console.warn('WebSocket is not connected');
            return false;
        }

        const message = {
            gameId,
            senderId: userId,
            content,
            type,
            targetId,
            timestamp: new Date().toISOString(),
        };

        wsRef.current.send(JSON.stringify(message));
        return true;
    }, [gameId, userId]);

    const sendPublicMessage = useCallback((content: string) => {
        return sendMessage(content, 'PUBLIC');
    }, [sendMessage]);

    const sendWhisper = useCallback((content: string, targetId: string) => {
        return sendMessage(content, 'WHISPER', targetId);
    }, [sendMessage]);

    useEffect(() => {
        if (gameId && userId) {
            connect();
        }

        return () => {
            disconnect();
        };
    }, [gameId, userId, connect, disconnect]);

    return {
        isConnected,
        sendPublicMessage,
        sendWhisper,
        disconnect,
        reconnect: connect,
    };
}
