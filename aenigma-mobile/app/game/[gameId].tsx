import { useState, useEffect, useRef, useCallback } from 'react';
import { StyleSheet, View, Text, ScrollView, TouchableOpacity, Alert, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { gameApi, Game, Clue, RoleDetail, AlibiEntry, tokenStorage } from '../../services/api';
import { useWebSocket, ChatMessage } from '../../services/useWebSocket';

export default function GameScreen() {
    const { gameId } = useLocalSearchParams<{ gameId: string }>();
    const [game, setGame] = useState<Game | null>(null);
    const [clues, setClues] = useState<Clue[]>([]);
    const [roleDetail, setRoleDetail] = useState<RoleDetail | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedTab, setSelectedTab] = useState<'role' | 'clues' | 'chat' | 'players'>('role');
    const [userId, setUserId] = useState<string>('');

    // 채팅 상태
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [newMessage, setNewMessage] = useState('');
    const scrollViewRef = useRef<ScrollView>(null);

    // WebSocket 메시지 핸들러
    const handleWebSocketMessage = useCallback((message: ChatMessage) => {
        setMessages(prev => [...prev, message]);
    }, []);

    // WebSocket 연결
    const { isConnected, sendPublicMessage } = useWebSocket({
        gameId: gameId || '',
        userId: userId,
        onMessage: handleWebSocketMessage,
    });

    useEffect(() => {
        const loadUserId = async () => {
            const id = await tokenStorage.getUserId();
            if (id) setUserId(id);
        };
        loadUserId();
    }, []);

    useEffect(() => {
        if (gameId) {
            loadGameData();
        }
    }, [gameId]);

    useEffect(() => {
        // 새 메시지 시 스크롤
        scrollViewRef.current?.scrollToEnd({ animated: true });
    }, [messages]);

    const loadGameData = async () => {
        try {
            setIsLoading(true);
            const [gameRes, cluesRes, roleRes] = await Promise.all([
                gameApi.get(gameId!),
                gameApi.getClues(gameId!),
                gameApi.getMyRole(gameId!),
            ]);
            setGame(gameRes.data);
            setClues(cluesRes.data);
            setRoleDetail(roleRes.data);

            // 시스템 메시지 추가
            setMessages([{
                id: 'sys-1',
                gameId: gameId!,
                senderId: 'system',
                senderNickname: '시스템',
                content: '게임에 입장했습니다. 역할을 확인하세요.',
                type: 'SYSTEM',
                timestamp: new Date().toISOString(),
            }]);
        } catch (error) {
            console.error('게임 데이터 로드 실패', error);
            Alert.alert('오류', '게임 정보를 불러올 수 없습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleSendMessage = () => {
        if (!newMessage.trim()) return;

        const sent = sendPublicMessage(newMessage);
        if (sent) {
            // 낙관적 UI 업데이트
            const message: ChatMessage = {
                id: `temp-${Date.now()}`,
                gameId: gameId!,
                senderId: userId,
                senderNickname: roleDetail?.nickname || '나',
                content: newMessage,
                type: 'PUBLIC',
                timestamp: new Date().toISOString(),
            };
            setMessages(prev => [...prev, message]);
        }
        setNewMessage('');
    };

    const getRoleEmoji = (role?: string) => {
        switch (role) {
            case 'CRIMINAL': return '🔪';
            case 'DETECTIVE': return '🔍';
            case 'CITIZEN': return '👤';
            default: return '❓';
        }
    };

    const getPhaseText = (phase?: string) => {
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

    const parseAlibi = (alibiJson?: string): AlibiEntry[] => {
        if (!alibiJson) return [];
        try {
            return JSON.parse(alibiJson);
        } catch {
            return [];
        }
    };

    if (isLoading || !game) {
        return (
            <View style={styles.loading}>
                <Text style={styles.loadingText}>게임 로딩 중...</Text>
            </View>
        );
    }

    return (
        <KeyboardAvoidingView
            style={styles.container}
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            keyboardVerticalOffset={90}
        >
            {/* Game Header */}
            <View style={styles.header}>
                <View style={styles.phaseInfo}>
                    <Text style={styles.phaseBadge}>{getPhaseText(game.phase)}</Text>
                    {game.phase === 'INVESTIGATION' && (
                        <Text style={styles.roundText}>
                            라운드 {game.investigationRound} / {game.maxInvestigationRounds}
                        </Text>
                    )}
                    <View style={[styles.wsStatus, { backgroundColor: isConnected ? '#22c55e' : '#ef4444' }]} />
                </View>
            </View>

            {/* Tab Navigation */}
            <View style={styles.tabs}>
                {['role', 'clues', 'chat', 'players'].map((tab) => (
                    <TouchableOpacity
                        key={tab}
                        style={[styles.tab, selectedTab === tab && styles.tabActive]}
                        onPress={() => setSelectedTab(tab as typeof selectedTab)}
                    >
                        <Text style={[styles.tabText, selectedTab === tab && styles.tabTextActive]}>
                            {tab === 'role' ? '🎭' : tab === 'clues' ? '📋' : tab === 'chat' ? '💬' : '👥'}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>

            {/* Tab Content */}
            {selectedTab === 'chat' ? (
                <View style={styles.chatContainer}>
                    <ScrollView
                        ref={scrollViewRef}
                        style={styles.chatMessages}
                        contentContainerStyle={styles.chatMessagesContent}
                    >
                        {messages.map((msg) => (
                            <View
                                key={msg.id}
                                style={[
                                    styles.messageItem,
                                    msg.type === 'SYSTEM' && styles.systemMessage,
                                    msg.senderId === userId && styles.myMessage,
                                ]}
                            >
                                {msg.type !== 'SYSTEM' && (
                                    <Text style={styles.messageSender}>{msg.senderNickname}</Text>
                                )}
                                <Text style={[
                                    styles.messageContent,
                                    msg.type === 'SYSTEM' && styles.systemMessageText,
                                    msg.senderId === userId && styles.myMessageText,
                                ]}>
                                    {msg.content}
                                </Text>
                            </View>
                        ))}
                    </ScrollView>

                    <View style={styles.chatInputContainer}>
                        <TextInput
                            style={styles.chatInput}
                            placeholder="메시지 입력..."
                            placeholderTextColor="#64748b"
                            value={newMessage}
                            onChangeText={setNewMessage}
                            onSubmitEditing={handleSendMessage}
                            returnKeyType="send"
                        />
                        <TouchableOpacity style={styles.sendButton} onPress={handleSendMessage}>
                            <Text style={styles.sendButtonText}>전송</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            ) : (
                <ScrollView style={styles.content}>
                    {selectedTab === 'role' && roleDetail && (
                        <View style={styles.roleSection}>
                            <View style={styles.roleCard}>
                                <Text style={styles.roleEmoji}>{getRoleEmoji(roleDetail.roleType)}</Text>
                                <Text style={styles.roleName}>{roleDetail.roleName || roleDetail.roleType}</Text>
                            </View>

                            {roleDetail.objective && (
                                <View style={styles.infoCard}>
                                    <Text style={styles.infoTitle}>🎯 목표</Text>
                                    <Text style={styles.infoText}>{roleDetail.objective}</Text>
                                </View>
                            )}

                            {roleDetail.description && (
                                <View style={styles.infoCard}>
                                    <Text style={styles.infoTitle}>📖 설명</Text>
                                    <Text style={styles.infoText}>{roleDetail.description}</Text>
                                </View>
                            )}

                            {roleDetail.secretInfo && (
                                <View style={[styles.infoCard, styles.secretCard]}>
                                    <Text style={styles.infoTitle}>🔐 비밀 정보</Text>
                                    <Text style={styles.infoText}>{roleDetail.secretInfo}</Text>
                                </View>
                            )}

                            {roleDetail.alibi && (
                                <View style={[styles.infoCard, styles.alibiCard]}>
                                    <Text style={styles.infoTitle}>📅 사건 당일 알리바이</Text>
                                    {parseAlibi(roleDetail.alibi).map((entry, idx) => (
                                        <View key={idx} style={styles.alibiEntry}>
                                            <Text style={styles.alibiTime}>{entry.time}</Text>
                                            <View>
                                                <Text style={styles.alibiLocation}>📍 {entry.location}</Text>
                                                <Text style={styles.alibiActivity}>{entry.activity}</Text>
                                                {entry.witnesses && entry.witnesses.length > 0 && (
                                                    <Text style={styles.alibiWitnesses}>
                                                        👁️ 목격자: {entry.witnesses.join(', ')}
                                                    </Text>
                                                )}
                                            </View>
                                        </View>
                                    ))}
                                </View>
                            )}
                        </View>
                    )}

                    {selectedTab === 'clues' && (
                        <View style={styles.cluesSection}>
                            {clues.length > 0 ? clues.map((clue) => (
                                <View
                                    key={clue.id}
                                    style={[styles.clueCard, !clue.isDiscovered && styles.clueLocked]}
                                >
                                    <View style={styles.clueHeader}>
                                        <Text style={styles.clueType}>
                                            {clue.clueType === 'PUBLIC' ? '🔍 공개' : clue.clueType === 'PERSONAL' ? '🎭 개인' : '🔒 비밀'}
                                        </Text>
                                        <Text style={styles.clueStatus}>
                                            {clue.isDiscovered ? '✅' : '🔒'}
                                        </Text>
                                    </View>
                                    <Text style={styles.clueTitle}>
                                        {clue.isDiscovered ? clue.title : '???'}
                                    </Text>
                                    <Text style={styles.clueContent}>
                                        {clue.isDiscovered ? clue.content : '아직 발견되지 않은 단서입니다.'}
                                    </Text>
                                </View>
                            )) : (
                                <Text style={styles.emptyText}>아직 단서가 없습니다.</Text>
                            )}
                        </View>
                    )}

                    {selectedTab === 'players' && (
                        <View style={styles.playersSection}>
                            {game.players.map((player) => (
                                <View
                                    key={player.id}
                                    style={[styles.playerCard, !player.isAlive && styles.playerDead]}
                                >
                                    <View style={styles.playerAvatar}>
                                        <Text style={styles.playerAvatarText}>
                                            {player.nickname[0].toUpperCase()}
                                        </Text>
                                    </View>
                                    <View style={styles.playerInfo}>
                                        <Text style={styles.playerName}>{player.nickname}</Text>
                                        <Text style={styles.playerTag}>{player.displayTag}</Text>
                                    </View>
                                    {!player.isAlive && <Text style={styles.deadBadge}>💀</Text>}
                                </View>
                            ))}
                        </View>
                    )}
                </ScrollView>
            )}
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    loading: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#0f0f1e',
    },
    loadingText: {
        color: '#64748b',
        fontSize: 16,
    },
    header: {
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    phaseInfo: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
    },
    phaseBadge: {
        backgroundColor: '#8a2be2',
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 8,
        color: '#fff',
        fontWeight: '600',
        overflow: 'hidden',
    },
    roundText: {
        color: '#64748b',
    },
    wsStatus: {
        width: 10,
        height: 10,
        borderRadius: 5,
    },
    tabs: {
        flexDirection: 'row',
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    tab: {
        flex: 1,
        paddingVertical: 12,
        alignItems: 'center',
    },
    tabActive: {
        borderBottomWidth: 2,
        borderBottomColor: '#8a2be2',
    },
    tabText: {
        fontSize: 20,
    },
    tabTextActive: {
        opacity: 1,
    },
    content: {
        flex: 1,
        padding: 16,
    },
    // Chat Styles
    chatContainer: {
        flex: 1,
    },
    chatMessages: {
        flex: 1,
    },
    chatMessagesContent: {
        padding: 12,
        gap: 8,
    },
    messageItem: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 10,
        maxWidth: '80%',
    },
    myMessage: {
        alignSelf: 'flex-end',
        backgroundColor: '#8a2be2',
    },
    systemMessage: {
        alignSelf: 'center',
        backgroundColor: 'transparent',
        maxWidth: '100%',
    },
    messageSender: {
        color: '#64748b',
        fontSize: 11,
        marginBottom: 2,
    },
    messageContent: {
        color: '#fff',
        fontSize: 14,
    },
    myMessageText: {
        color: '#fff',
    },
    systemMessageText: {
        color: '#64748b',
        fontStyle: 'italic',
        textAlign: 'center',
    },
    chatInputContainer: {
        flexDirection: 'row',
        padding: 12,
        gap: 8,
        borderTopWidth: 1,
        borderTopColor: '#1e1e3f',
        backgroundColor: '#0f0f1e',
    },
    chatInput: {
        flex: 1,
        backgroundColor: '#1a1a2e',
        borderRadius: 20,
        paddingHorizontal: 16,
        paddingVertical: 10,
        color: '#fff',
        fontSize: 14,
    },
    sendButton: {
        backgroundColor: '#8a2be2',
        borderRadius: 20,
        paddingHorizontal: 20,
        justifyContent: 'center',
    },
    sendButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
    // Role Section
    roleSection: {
        gap: 16,
    },
    roleCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 16,
        padding: 24,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#8a2be2',
    },
    roleEmoji: {
        fontSize: 48,
        marginBottom: 12,
    },
    roleName: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
    },
    infoCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 16,
    },
    secretCard: {
        backgroundColor: 'rgba(236, 72, 153, 0.1)',
        borderLeftWidth: 3,
        borderLeftColor: '#ec4899',
    },
    alibiCard: {
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        borderLeftWidth: 3,
        borderLeftColor: '#3b82f6',
    },
    infoTitle: {
        fontSize: 14,
        fontWeight: '600',
        color: '#fff',
        marginBottom: 8,
    },
    infoText: {
        color: '#94a3b8',
        lineHeight: 22,
    },
    alibiEntry: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 8,
        paddingTop: 8,
        borderTopWidth: 1,
        borderTopColor: 'rgba(255,255,255,0.05)',
    },
    alibiTime: {
        color: '#3b82f6',
        fontWeight: '600',
        width: 50,
    },
    alibiLocation: {
        color: '#fff',
        fontSize: 13,
    },
    alibiActivity: {
        color: '#94a3b8',
        fontSize: 13,
    },
    alibiWitnesses: {
        color: '#64748b',
        fontSize: 12,
        fontStyle: 'italic',
    },
    cluesSection: {
        gap: 12,
    },
    clueCard: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 16,
    },
    clueLocked: {
        opacity: 0.6,
    },
    clueHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 8,
    },
    clueType: {
        color: '#64748b',
        fontSize: 12,
    },
    clueStatus: {
        fontSize: 14,
    },
    clueTitle: {
        color: '#fff',
        fontWeight: '600',
        marginBottom: 4,
    },
    clueContent: {
        color: '#94a3b8',
        fontSize: 14,
    },
    emptyText: {
        color: '#64748b',
        textAlign: 'center',
        marginTop: 40,
    },
    playersSection: {
        gap: 8,
    },
    playerCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 12,
    },
    playerDead: {
        opacity: 0.5,
    },
    playerAvatar: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: '#8a2be2',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    playerAvatarText: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 18,
    },
    playerInfo: {
        flex: 1,
    },
    playerName: {
        color: '#fff',
        fontWeight: '600',
    },
    playerTag: {
        color: '#64748b',
        fontSize: 12,
    },
    deadBadge: {
        fontSize: 20,
    },
});
