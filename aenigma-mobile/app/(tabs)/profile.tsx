import React from 'react';
import {
    StyleSheet,
    View,
    Text,
    TouchableOpacity,
    Alert,
} from 'react-native';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { useRouter } from 'expo-router';
import { useAuth } from '../../contexts';

export default function ProfileScreen() {
    const router = useRouter();
    const { user, logout, isLoggedIn } = useAuth();

    const handleLogout = async () => {
        Alert.alert(
            '로그아웃',
            '정말 로그아웃 하시겠습니까?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '로그아웃',
                    style: 'destructive',
                    onPress: async () => {
                        await logout();
                        router.replace('/(auth)/login');
                    },
                },
            ]
        );
    };

    if (!isLoggedIn) {
        return (
            <View style={styles.container}>
                <View style={styles.loginPrompt}>
                    <Text style={styles.promptText}>로그인이 필요합니다</Text>
                    <TouchableOpacity
                        style={styles.loginButton}
                        onPress={() => router.replace('/(auth)/login')}
                    >
                        <Text style={styles.loginButtonText}>로그인하기</Text>
                    </TouchableOpacity>
                </View>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            {/* Profile Header */}
            <View style={styles.profileHeader}>
                <View style={styles.avatar}>
                    <FontAwesome name="user" size={40} color="#e94560" />
                </View>
                <Text style={styles.displayName}>{user?.displayName}</Text>
                <Text style={styles.username}>@{user?.username}</Text>
                <View style={styles.roleBadge}>
                    <Text style={styles.roleText}>{user?.role}</Text>
                </View>
            </View>

            {/* Stats */}
            <View style={styles.statsSection}>
                <View style={styles.statItem}>
                    <Text style={styles.statValue}>0</Text>
                    <Text style={styles.statLabel}>총 게임</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                    <Text style={styles.statValue}>0%</Text>
                    <Text style={styles.statLabel}>승률</Text>
                </View>
                <View style={styles.statDivider} />
                <View style={styles.statItem}>
                    <Text style={styles.statValue}>0</Text>
                    <Text style={styles.statLabel}>승리</Text>
                </View>
            </View>

            {/* Menu */}
            <View style={styles.menuSection}>
                <TouchableOpacity style={styles.menuItem}>
                    <FontAwesome name="history" size={20} color="#888" />
                    <Text style={styles.menuText}>게임 기록</Text>
                    <FontAwesome name="chevron-right" size={14} color="#444" />
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem}>
                    <FontAwesome name="cog" size={20} color="#888" />
                    <Text style={styles.menuText}>설정</Text>
                    <FontAwesome name="chevron-right" size={14} color="#444" />
                </TouchableOpacity>

                <TouchableOpacity style={styles.menuItem}>
                    <FontAwesome name="info-circle" size={20} color="#888" />
                    <Text style={styles.menuText}>앱 정보</Text>
                    <FontAwesome name="chevron-right" size={14} color="#444" />
                </TouchableOpacity>

                <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
                    <FontAwesome name="sign-out" size={20} color="#e94560" />
                    <Text style={styles.logoutText}>로그아웃</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#1a1a2e',
    },
    loginPrompt: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    promptText: {
        fontSize: 16,
        color: '#888',
        marginBottom: 16,
    },
    loginButton: {
        backgroundColor: '#e94560',
        paddingHorizontal: 24,
        paddingVertical: 12,
        borderRadius: 12,
    },
    loginButtonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    profileHeader: {
        alignItems: 'center',
        paddingVertical: 32,
        borderBottomWidth: 1,
        borderBottomColor: '#0f3460',
    },
    avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: '#16213e',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
        borderWidth: 2,
        borderColor: '#e94560',
    },
    displayName: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 4,
    },
    username: {
        fontSize: 14,
        color: '#888',
        marginBottom: 12,
    },
    roleBadge: {
        backgroundColor: 'rgba(233, 69, 96, 0.2)',
        paddingHorizontal: 12,
        paddingVertical: 4,
        borderRadius: 8,
    },
    roleText: {
        fontSize: 12,
        color: '#e94560',
    },
    statsSection: {
        flexDirection: 'row',
        padding: 24,
        backgroundColor: '#16213e',
        marginHorizontal: 16,
        marginTop: 16,
        borderRadius: 16,
    },
    statItem: {
        flex: 1,
        alignItems: 'center',
    },
    statValue: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 4,
    },
    statLabel: {
        fontSize: 12,
        color: '#888',
    },
    statDivider: {
        width: 1,
        backgroundColor: '#0f3460',
    },
    menuSection: {
        padding: 16,
        gap: 4,
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#16213e',
        padding: 16,
        borderRadius: 12,
        marginBottom: 8,
    },
    menuText: {
        flex: 1,
        marginLeft: 16,
        fontSize: 16,
        color: '#fff',
    },
    logoutButton: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(233, 69, 96, 0.1)',
        padding: 16,
        borderRadius: 12,
        marginTop: 16,
        justifyContent: 'center',
    },
    logoutText: {
        marginLeft: 12,
        fontSize: 16,
        color: '#e94560',
        fontWeight: '600',
    },
});
