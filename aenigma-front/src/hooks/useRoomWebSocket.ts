import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8081/ws';

export interface RoomMember {
    id: string;
    nickname: string;
    displayTag?: string;
    isHost: boolean;
    isReady: boolean;
}

export interface RoomMessage {
    id: string;
    roomId: string;
    senderId: string;
    senderNickname: string;
    content: string;
    type: 'CHAT' | 'SYSTEM' | 'JOIN' | 'LEAVE' | 'READY' | 'GAME_START';
    timestamp: string;
}

export interface RoomEvent {
    type: 'MEMBER_JOIN' | 'MEMBER_LEAVE' | 'MEMBER_READY' | 'MEMBER_UNREADY' | 'GAME_STARTED' | 'ROOM_CLOSED';
    member?: RoomMember;
    members?: RoomMember[];
    gameId?: string;
    message?: string;
}

interface UseRoomWebSocketOptions {
    roomId: string;
    userId: string;
    onMessage?: (message: RoomMessage) => void;
    onMemberUpdate?: (members: RoomMember[]) => void;
    onRoomEvent?: (event: RoomEvent) => void;
    onConnect?: () => void;
    onDisconnect?: () => void;
    onError?: (error: string) => void;
}

export function useRoomWebSocket({
    roomId,
    userId,
    onMessage,
    onMemberUpdate,
    onRoomEvent,
    onConnect,
    onDisconnect,
    onError,
}: UseRoomWebSocketOptions) {
    const clientRef = useRef<Client | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [connectionError, setConnectionError] = useState<string | null>(null);
    const [members, setMembers] = useState<RoomMember[]>([]);

    useEffect(() => {
        if (!roomId || !userId) return;

        const client = new Client({
            webSocketFactory: () => new SockJS(WS_URL),
            connectHeaders: {
                'X-User-Id': userId,
            },
            debug: (str) => {
                if (import.meta.env.DEV) {
                    console.log('[STOMP Room]', str);
                }
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log('Room WebSocket 연결됨');
            setIsConnected(true);
            setConnectionError(null);
            onConnect?.();

            // 방 채팅 구독
            client.subscribe(`/topic/room/${roomId}`, (message: IMessage) => {
                try {
                    const roomMessage: RoomMessage = JSON.parse(message.body);
                    onMessage?.(roomMessage);
                } catch (e) {
                    console.error('방 메시지 파싱 실패', e);
                }
            });

            // 방 이벤트 구독 (멤버 입장/퇴장, 준비 상태 등)
            client.subscribe(`/topic/room/${roomId}/events`, (message: IMessage) => {
                try {
                    const event: RoomEvent = JSON.parse(message.body);
                    handleRoomEvent(event);
                } catch (e) {
                    console.error('방 이벤트 파싱 실패', e);
                }
            });

            // 방 멤버 목록 구독
            client.subscribe(`/topic/room/${roomId}/members`, (message: IMessage) => {
                try {
                    const memberList: RoomMember[] = JSON.parse(message.body);
                    setMembers(memberList);
                    onMemberUpdate?.(memberList);
                } catch (e) {
                    console.error('멤버 목록 파싱 실패', e);
                }
            });

            // 방 입장 알림 전송
            client.publish({
                destination: '/app/room/join',
                body: JSON.stringify({ roomId, userId }),
            });
        };

        const handleRoomEvent = (event: RoomEvent) => {
            onRoomEvent?.(event);

            switch (event.type) {
                case 'MEMBER_JOIN':
                    if (event.member) {
                        setMembers(prev => {
                            if (prev.find(m => m.id === event.member!.id)) return prev;
                            return [...prev, event.member!];
                        });
                    }
                    break;
                case 'MEMBER_LEAVE':
                    if (event.member) {
                        setMembers(prev => prev.filter(m => m.id !== event.member!.id));
                    }
                    break;
                case 'MEMBER_READY':
                case 'MEMBER_UNREADY':
                    if (event.member) {
                        setMembers(prev => prev.map(m =>
                            m.id === event.member!.id ? { ...m, isReady: event.type === 'MEMBER_READY' } : m
                        ));
                    }
                    break;
                case 'GAME_STARTED':
                    // 게임 시작 시 처리
                    break;
                case 'ROOM_CLOSED':
                    // 방 닫힘 시 처리
                    break;
            }
        };

        client.onDisconnect = () => {
            console.log('Room WebSocket 연결 해제됨');
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
            // 방 퇴장 알림 전송
            if (client.connected) {
                client.publish({
                    destination: '/app/room/leave',
                    body: JSON.stringify({ roomId, userId }),
                });
            }
            client.deactivate();
            clientRef.current = null;
        };
    }, [roomId, userId]);

    // 채팅 메시지 전송
    const sendMessage = useCallback((content: string) => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket 연결되지 않음');
            return false;
        }

        clientRef.current.publish({
            destination: '/app/room/chat',
            body: JSON.stringify({
                roomId,
                senderId: userId,
                content,
                type: 'CHAT',
            }),
        });

        return true;
    }, [roomId, userId]);

    // 준비 상태 토글
    const toggleReady = useCallback(() => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket 연결되지 않음');
            return false;
        }

        clientRef.current.publish({
            destination: '/app/room/ready',
            body: JSON.stringify({ roomId, userId }),
        });

        return true;
    }, [roomId, userId]);

    // 게임 시작 (방장 전용)
    const startGame = useCallback((scenarioId?: string) => {
        if (!clientRef.current?.connected) {
            console.error('WebSocket 연결되지 않음');
            return false;
        }

        clientRef.current.publish({
            destination: '/app/room/start-game',
            body: JSON.stringify({ roomId, scenarioId }),
        });

        return true;
    }, [roomId]);

    return {
        isConnected,
        connectionError,
        members,
        sendMessage,
        toggleReady,
        startGame,
    };
}
