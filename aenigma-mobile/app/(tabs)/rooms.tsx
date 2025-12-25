import React, { useState, useEffect } from 'react';
import {
    StyleSheet,
    View,
    Text,
    FlatList,
    TouchableOpacity,
    RefreshControl,
    TextInput,
    Alert,
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { RoomService, RoomResponse } from '../../services';

export default function RoomsScreen() {
    const [rooms, setRooms] = useState<RoomResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [roomCode, setRoomCode] = useState('');

    useEffect(() => {
        loadRooms();
    }, []);

    const loadRooms = async () => {
        setIsLoading(true);
        try {
            const data = await RoomService.getJoinableRooms();
            setRooms(data);
        } catch (error) {
            console.error('Failed to load rooms:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleJoinByCode = async () => {
        if (!roomCode.trim()) {
            Alert.alert('오류', '방 코드를 입력해주세요.');
            return;
        }

        try {
            await RoomService.joinRoom(roomCode.trim());
            Alert.alert('성공', '방에 입장했습니다!');
            setRoomCode('');
        } catch (error: any) {
            Alert.alert('입장 실패', error.message || '방을 찾을 수 없습니다.');
        }
    };

    const handleJoinRoom = async (room: RoomResponse) => {
        try {
            if (room.isPrivate) {
                // TODO: Show password input modal
                Alert.alert('비밀번호 필요', '비밀번호가 필요한 방입니다.');
                return;
            }
            await RoomService.joinRoom(room.roomCode);
            Alert.alert('성공', `${room.title}에 입장했습니다!`);
        } catch (error: any) {
            Alert.alert('입장 실패', error.message || '다시 시도해주세요.');
        }
    };

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'WAITING':
                return styles.statusWaiting;
            case 'PLAYING':
                return styles.statusPlaying;
            default:
                return {};
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'WAITING':
                return '대기중';
            case 'PLAYING':
                return '진행중';
            case 'FINISHED':
                return '종료';
            default:
                return status;
        }
    };

    const renderRoom = ({ item }: { item: RoomResponse }) => (
        <TouchableOpacity
            style={styles.roomCard}
            onPress={() => handleJoinRoom(item)}
        >
            <View style={styles.roomHeader}>
                <Text style={styles.roomTitle}>{item.title}</Text>
                {item.isPrivate && (
                    <FontAwesome name="lock" size={16} color="#e94560" />
                )}
            </View>
            <View style={styles.roomInfo}>
                <Text style={styles.roomHost}>
                    <FontAwesome name="user" size={12} color="#888" /> {item.host.displayName}
                </Text>
                <Text style={styles.roomPlayers}>
                    <FontAwesome name="users" size={12} color="#888" /> {item.currentPlayers}/{item.maxPlayers}
                </Text>
            </View>
            <View style={styles.roomFooter}>
                <Text style={styles.roomCode}>#{item.roomCode}</Text>
                <View style={[styles.statusBadge, getStatusStyle(item.status)]}>
                    <Text style={styles.statusText}>
                        {getStatusText(item.status)}
                    </Text>
                </View>
            </View>
        </TouchableOpacity>
    );

    return (
        <View style={styles.container}>
            {/* Room Code Input */}
            <View style={styles.codeInputSection}>
                <TextInput
                    style={styles.codeInput}
                    placeholder="방 코드 입력"
                    placeholderTextColor="#666"
                    value={roomCode}
                    onChangeText={setRoomCode}
                    autoCapitalize="characters"
                />
                <TouchableOpacity style={styles.joinButton} onPress={handleJoinByCode}>
                    <FontAwesome name="sign-in" size={20} color="#fff" />
                </TouchableOpacity>
            </View>

            {/* Room List */}
            <FlatList
                data={rooms}
                renderItem={renderRoom}
                keyExtractor={(item) => item.id}
                contentContainerStyle={styles.listContent}
                refreshControl={
                    <RefreshControl
                        refreshing={isLoading}
                        onRefresh={loadRooms}
                        tintColor="#e94560"
                    />
                }
                ListEmptyComponent={
                    <View style={styles.emptyState}>
                        <FontAwesome name="inbox" size={48} color="#444" />
                        <Text style={styles.emptyText}>입장 가능한 방이 없습니다</Text>
                        <Text style={styles.emptySubtext}>새로운 방을 만들어보세요!</Text>
                    </View>
                }
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#1a1a2e',
    },
    codeInputSection: {
        flexDirection: 'row',
        padding: 16,
        gap: 12,
    },
    codeInput: {
        flex: 1,
        backgroundColor: '#16213e',
        borderRadius: 12,
        padding: 14,
        fontSize: 16,
        color: '#fff',
        borderWidth: 1,
        borderColor: '#0f3460',
    },
    joinButton: {
        backgroundColor: '#e94560',
        borderRadius: 12,
        padding: 14,
        justifyContent: 'center',
        alignItems: 'center',
        width: 50,
    },
    listContent: {
        padding: 16,
        paddingTop: 0,
    },
    roomCard: {
        backgroundColor: '#16213e',
        borderRadius: 16,
        padding: 16,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: '#0f3460',
    },
    roomHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 8,
    },
    roomTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#fff',
        flex: 1,
    },
    roomInfo: {
        flexDirection: 'row',
        gap: 16,
        marginBottom: 12,
    },
    roomHost: {
        fontSize: 14,
        color: '#888',
    },
    roomPlayers: {
        fontSize: 14,
        color: '#888',
    },
    roomFooter: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    roomCode: {
        fontSize: 12,
        color: '#666',
        fontFamily: 'SpaceMono',
    },
    statusBadge: {
        paddingHorizontal: 10,
        paddingVertical: 4,
        borderRadius: 8,
    },
    statusWaiting: {
        backgroundColor: 'rgba(76, 175, 80, 0.2)',
    },
    statusPlaying: {
        backgroundColor: 'rgba(233, 69, 96, 0.2)',
    },
    statusText: {
        fontSize: 12,
        color: '#4caf50',
    },
    emptyState: {
        alignItems: 'center',
        paddingVertical: 48,
    },
    emptyText: {
        fontSize: 16,
        color: '#888',
        marginTop: 16,
    },
    emptySubtext: {
        fontSize: 14,
        color: '#666',
        marginTop: 4,
    },
});
