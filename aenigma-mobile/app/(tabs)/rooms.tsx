import { useState, useEffect } from 'react';
import { StyleSheet, View, Text, FlatList, TouchableOpacity, RefreshControl, TextInput, Alert } from 'react-native';
import { router } from 'expo-router';
import { roomApi, Room, CreateRoomRequest } from '../../services/api';

export default function RoomsScreen() {
    const [rooms, setRooms] = useState<Room[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isCreating, setIsCreating] = useState(false);
    const [newRoomTitle, setNewRoomTitle] = useState('');

    useEffect(() => {
        loadRooms();
    }, []);

    const loadRooms = async () => {
        try {
            setIsLoading(true);
            const response = await roomApi.joinable();
            setRooms(response.data);
        } catch (error) {
            console.error('방 목록 로드 실패', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleJoinRoom = async (room: Room) => {
        try {
            await roomApi.join({ roomCode: room.roomCode });
            router.push(`/room/${room.id}`);
        } catch (error) {
            Alert.alert('오류', '방 입장에 실패했습니다.');
        }
    };

    const handleCreateRoom = async () => {
        if (!newRoomTitle.trim()) {
            Alert.alert('오류', '방 제목을 입력해주세요.');
            return;
        }

        try {
            const request: CreateRoomRequest = {
                title: newRoomTitle,
                maxPlayers: 8,
            };
            const response = await roomApi.create(request);
            setIsCreating(false);
            setNewRoomTitle('');
            router.push(`/room/${response.data.id}`);
        } catch (error) {
            Alert.alert('오류', '방 생성에 실패했습니다.');
        }
    };

    const getStatusColor = (status: Room['status']) => {
        switch (status) {
            case 'WAITING': return '#22c55e';
            case 'PLAYING': return '#f59e0b';
            default: return '#64748b';
        }
    };

    const getStatusText = (status: Room['status']) => {
        switch (status) {
            case 'WAITING': return '대기중';
            case 'PLAYING': return '게임중';
            case 'FINISHED': return '종료됨';
            default: return status;
        }
    };

    const renderRoom = ({ item }: { item: Room }) => (
        <TouchableOpacity
            style={styles.roomCard}
            onPress={() => handleJoinRoom(item)}
            disabled={item.status !== 'WAITING'}
        >
            <View style={styles.roomHeader}>
                <Text style={styles.roomTitle}>{item.title}</Text>
                <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
                    <Text style={styles.statusText}>{getStatusText(item.status)}</Text>
                </View>
            </View>
            <View style={styles.roomInfo}>
                <Text style={styles.roomCode}>🔑 {item.roomCode}</Text>
                <Text style={styles.playerCount}>
                    👥 {item.currentPlayerCount} / {item.maxPlayers}
                </Text>
            </View>
            <Text style={styles.hostName}>호스트: {item.hostNickname}</Text>
        </TouchableOpacity>
    );

    return (
        <View style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.title}>🎮 방 목록</Text>
                <TouchableOpacity
                    style={styles.createButton}
                    onPress={() => setIsCreating(!isCreating)}
                >
                    <Text style={styles.createButtonText}>
                        {isCreating ? '취소' : '➕ 방 만들기'}
                    </Text>
                </TouchableOpacity>
            </View>

            {isCreating && (
                <View style={styles.createForm}>
                    <TextInput
                        style={styles.input}
                        placeholder="방 제목 입력..."
                        placeholderTextColor="#64748b"
                        value={newRoomTitle}
                        onChangeText={setNewRoomTitle}
                    />
                    <TouchableOpacity style={styles.submitButton} onPress={handleCreateRoom}>
                        <Text style={styles.submitButtonText}>생성하기</Text>
                    </TouchableOpacity>
                </View>
            )}

            <FlatList
                data={rooms}
                keyExtractor={(item) => item.id}
                renderItem={renderRoom}
                refreshControl={
                    <RefreshControl refreshing={isLoading} onRefresh={loadRooms} tintColor="#8a2be2" />
                }
                ListEmptyComponent={
                    <View style={styles.empty}>
                        <Text style={styles.emptyText}>참가 가능한 방이 없습니다.</Text>
                        <Text style={styles.emptySubtext}>새 방을 만들어보세요!</Text>
                    </View>
                }
                contentContainerStyle={rooms.length === 0 ? styles.emptyContainer : undefined}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: 16,
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    title: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#fff',
    },
    createButton: {
        backgroundColor: '#8a2be2',
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 8,
    },
    createButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
    createForm: {
        padding: 16,
        backgroundColor: '#1a1a2e',
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
        gap: 12,
    },
    input: {
        backgroundColor: '#0f0f1e',
        borderWidth: 1,
        borderColor: '#334155',
        borderRadius: 8,
        padding: 12,
        color: '#fff',
        fontSize: 16,
    },
    submitButton: {
        backgroundColor: '#22c55e',
        paddingVertical: 12,
        borderRadius: 8,
        alignItems: 'center',
    },
    submitButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
    roomCard: {
        backgroundColor: '#1a1a2e',
        margin: 12,
        marginBottom: 0,
        padding: 16,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: '#334155',
    },
    roomHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 8,
    },
    roomTitle: {
        fontSize: 16,
        fontWeight: '600',
        color: '#fff',
        flex: 1,
    },
    statusBadge: {
        paddingVertical: 4,
        paddingHorizontal: 8,
        borderRadius: 4,
    },
    statusText: {
        color: '#fff',
        fontSize: 12,
        fontWeight: '600',
    },
    roomInfo: {
        flexDirection: 'row',
        gap: 16,
        marginBottom: 8,
    },
    roomCode: {
        color: '#94a3b8',
        fontSize: 14,
    },
    playerCount: {
        color: '#94a3b8',
        fontSize: 14,
    },
    hostName: {
        color: '#64748b',
        fontSize: 12,
    },
    empty: {
        alignItems: 'center',
        justifyContent: 'center',
        padding: 40,
    },
    emptyContainer: {
        flex: 1,
    },
    emptyText: {
        color: '#64748b',
        fontSize: 16,
        marginBottom: 8,
    },
    emptySubtext: {
        color: '#475569',
        fontSize: 14,
    },
});
