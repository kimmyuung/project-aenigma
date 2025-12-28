import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, FlatList, TouchableOpacity, Alert } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { roomApi, Room, GamePlayer } from '../../services/api';

export default function RoomDetailScreen() {
    const { roomId } = useLocalSearchParams<{ roomId: string }>();
    const [room, setRoom] = useState<Room | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isStarting, setIsStarting] = useState(false);

    useEffect(() => {
        if (roomId) {
            loadRoom();
        }
    }, [roomId]);

    const loadRoom = async () => {
        try {
            setIsLoading(true);
            const response = await roomApi.get(roomId!);
            setRoom(response.data);
        } catch (error) {
            console.error('방 정보 로드 실패', error);
            Alert.alert('오류', '방 정보를 불러올 수 없습니다.');
            router.back();
        } finally {
            setIsLoading(false);
        }
    };

    const handleStartGame = async () => {
        try {
            setIsStarting(true);
            await roomApi.start(roomId!);
            // TODO: 게임 ID 받아서 게임 화면으로 이동
            Alert.alert('알림', '게임이 시작되었습니다!');
        } catch (error) {
            Alert.alert('오류', '게임 시작에 실패했습니다.');
        } finally {
            setIsStarting(false);
        }
    };

    const handleLeaveRoom = async () => {
        Alert.alert(
            '방 나가기',
            '정말 방을 나가시겠습니까?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '나가기',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await roomApi.leave(roomId!);
                            router.replace('/(tabs)/rooms');
                        } catch (error) {
                            Alert.alert('오류', '방 나가기에 실패했습니다.');
                        }
                    },
                },
            ]
        );
    };

    if (isLoading || !room) {
        return (
            <View style={styles.loading}>
                <Text style={styles.loadingText}>로딩 중...</Text>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.title}>{room.title}</Text>
                <View style={styles.roomInfo}>
                    <Text style={styles.roomCode}>🔑 {room.roomCode}</Text>
                    <Text style={styles.playerCount}>
                        👥 {room.currentPlayerCount} / {room.maxPlayers}
                    </Text>
                </View>
            </View>

            <View style={styles.statusSection}>
                <View style={[styles.statusBadge, { backgroundColor: room.status === 'WAITING' ? '#22c55e' : '#f59e0b' }]}>
                    <Text style={styles.statusText}>
                        {room.status === 'WAITING' ? '대기 중' : room.status}
                    </Text>
                </View>
                <Text style={styles.hostText}>호스트: {room.hostNickname}</Text>
            </View>

            <View style={styles.playersSection}>
                <Text style={styles.sectionTitle}>참가자</Text>
                <View style={styles.playersList}>
                    {/* TODO: 실제 플레이어 목록 표시 */}
                    <View style={styles.playerPlaceholder}>
                        <Text style={styles.placeholderText}>
                            {room.currentPlayerCount}명 참가 중
                        </Text>
                    </View>
                </View>
            </View>

            <View style={styles.actions}>
                <TouchableOpacity
                    style={[styles.startButton, isStarting && styles.buttonDisabled]}
                    onPress={handleStartGame}
                    disabled={isStarting || room.status !== 'WAITING'}
                >
                    <Text style={styles.startButtonText}>
                        {isStarting ? '시작 중...' : '🎮 게임 시작'}
                    </Text>
                </TouchableOpacity>

                <TouchableOpacity style={styles.leaveButton} onPress={handleLeaveRoom}>
                    <Text style={styles.leaveButtonText}>🚪 방 나가기</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
        padding: 16,
    },
    loading: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#0f0f1e',
    },
    loadingText: {
        color: '#64748b',
    },
    header: {
        marginBottom: 24,
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 8,
    },
    roomInfo: {
        flexDirection: 'row',
        gap: 16,
    },
    roomCode: {
        color: '#8a2be2',
        fontWeight: '600',
    },
    playerCount: {
        color: '#94a3b8',
    },
    statusSection: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        marginBottom: 24,
    },
    statusBadge: {
        paddingVertical: 6,
        paddingHorizontal: 12,
        borderRadius: 6,
    },
    statusText: {
        color: '#fff',
        fontWeight: '600',
    },
    hostText: {
        color: '#64748b',
    },
    playersSection: {
        flex: 1,
    },
    sectionTitle: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
        marginBottom: 12,
    },
    playersList: {
        backgroundColor: '#1a1a2e',
        borderRadius: 12,
        padding: 16,
    },
    playerPlaceholder: {
        alignItems: 'center',
        padding: 20,
    },
    placeholderText: {
        color: '#64748b',
    },
    actions: {
        gap: 12,
        marginTop: 16,
    },
    startButton: {
        backgroundColor: '#22c55e',
        paddingVertical: 16,
        borderRadius: 12,
        alignItems: 'center',
    },
    buttonDisabled: {
        opacity: 0.6,
    },
    startButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    leaveButton: {
        borderWidth: 1,
        borderColor: '#ef4444',
        paddingVertical: 14,
        borderRadius: 12,
        alignItems: 'center',
    },
    leaveButtonText: {
        color: '#ef4444',
        fontSize: 16,
    },
});
