import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { useAuth } from '../../contexts/AuthContext';
import { router } from 'expo-router';

export default function HomeScreen() {
    const { user, isAuthenticated } = useAuth();

    const handleJoinRoom = () => {
        router.push('/rooms');
    };

    const handleCreateRoom = () => {
        router.push('/rooms?create=true');
    };

    return (
        <View style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.logo}>🎭</Text>
                <Text style={styles.title}>AENIGMA</Text>
                <Text style={styles.subtitle}>온라인 머더미스터리 게임</Text>
            </View>

            {isAuthenticated ? (
                <View style={styles.content}>
                    <Text style={styles.welcome}>
                        안녕하세요, {user?.nickname || user?.username}님!
                    </Text>

                    <View style={styles.buttonContainer}>
                        <TouchableOpacity style={styles.primaryButton} onPress={handleJoinRoom}>
                            <Text style={styles.buttonText}>🎮 방 참가하기</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.secondaryButton} onPress={handleCreateRoom}>
                            <Text style={styles.secondaryButtonText}>➕ 방 만들기</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            ) : (
                <View style={styles.content}>
                    <TouchableOpacity style={styles.primaryButton} onPress={() => router.push('/(auth)/login')}>
                        <Text style={styles.buttonText}>로그인하기</Text>
                    </TouchableOpacity>
                </View>
            )}

            <View style={styles.features}>
                <Text style={styles.featureTitle}>🕵️ 게임 특징</Text>
                <View style={styles.featureItem}>
                    <Text style={styles.featureEmoji}>🎭</Text>
                    <Text style={styles.featureText}>역할 기반 추리</Text>
                </View>
                <View style={styles.featureItem}>
                    <Text style={styles.featureEmoji}>🎧</Text>
                    <Text style={styles.featureText}>Discord 음성 채팅</Text>
                </View>
                <View style={styles.featureItem}>
                    <Text style={styles.featureEmoji}>📱</Text>
                    <Text style={styles.featureText}>모바일로 어디서나</Text>
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
        padding: 20,
    },
    header: {
        alignItems: 'center',
        marginTop: 40,
        marginBottom: 40,
    },
    logo: {
        fontSize: 64,
        marginBottom: 10,
    },
    title: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#fff',
        letterSpacing: 4,
    },
    subtitle: {
        fontSize: 14,
        color: '#64748b',
        marginTop: 8,
    },
    content: {
        alignItems: 'center',
        marginBottom: 40,
    },
    welcome: {
        fontSize: 18,
        color: '#fff',
        marginBottom: 24,
    },
    buttonContainer: {
        width: '100%',
        gap: 12,
    },
    primaryButton: {
        backgroundColor: '#8a2be2',
        paddingVertical: 16,
        paddingHorizontal: 32,
        borderRadius: 12,
        alignItems: 'center',
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    secondaryButton: {
        borderWidth: 1,
        borderColor: '#8a2be2',
        paddingVertical: 16,
        paddingHorizontal: 32,
        borderRadius: 12,
        alignItems: 'center',
    },
    secondaryButtonText: {
        color: '#8a2be2',
        fontSize: 16,
        fontWeight: '600',
    },
    features: {
        marginTop: 20,
    },
    featureTitle: {
        fontSize: 18,
        fontWeight: '600',
        color: '#fff',
        marginBottom: 16,
    },
    featureItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        marginBottom: 12,
        backgroundColor: '#1a1a2e',
        padding: 12,
        borderRadius: 8,
    },
    featureEmoji: {
        fontSize: 24,
    },
    featureText: {
        color: '#94a3b8',
        fontSize: 14,
    },
});
