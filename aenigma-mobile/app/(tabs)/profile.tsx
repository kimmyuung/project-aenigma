import { StyleSheet, View, Text, TouchableOpacity, Alert } from 'react-native';
import { router } from 'expo-router';
import { useAuth } from '../../contexts/AuthContext';

export default function ProfileScreen() {
    const { user, logout, isAuthenticated } = useAuth();

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

    if (!isAuthenticated) {
        return (
            <View style={styles.container}>
                <View style={styles.notLoggedIn}>
                    <Text style={styles.notLoggedInText}>로그인이 필요합니다</Text>
                    <TouchableOpacity
                        style={styles.loginButton}
                        onPress={() => router.push('/(auth)/login')}
                    >
                        <Text style={styles.loginButtonText}>로그인하기</Text>
                    </TouchableOpacity>
                </View>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <View style={styles.profileHeader}>
                <View style={styles.avatar}>
                    <Text style={styles.avatarText}>
                        {user?.nickname?.[0]?.toUpperCase() || '?'}
                    </Text>
                </View>
                <Text style={styles.nickname}>{user?.nickname}</Text>
                <Text style={styles.displayTag}>{user?.displayTag}</Text>
            </View>

            <View style={styles.stats}>
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>0</Text>
                    <Text style={styles.statLabel}>플레이</Text>
                </View>
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>0</Text>
                    <Text style={styles.statLabel}>승리</Text>
                </View>
                <View style={styles.statItem}>
                    <Text style={styles.statNumber}>0%</Text>
                    <Text style={styles.statLabel}>승률</Text>
                </View>
            </View>

            <View style={styles.menu}>
                <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>📊 게임 기록</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>⚙️ 설정</Text>
                </TouchableOpacity>
                <TouchableOpacity style={styles.menuItem}>
                    <Text style={styles.menuText}>📖 도움말</Text>
                </TouchableOpacity>
                <TouchableOpacity style={[styles.menuItem, styles.logoutItem]} onPress={handleLogout}>
                    <Text style={styles.logoutText}>🚪 로그아웃</Text>
                </TouchableOpacity>
            </View>

            <View style={styles.footer}>
                <Text style={styles.footerText}>AENIGMA v1.0.0</Text>
                <Text style={styles.footerLink}>github.com/kimmyuung</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    notLoggedIn: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
    },
    notLoggedInText: {
        color: '#64748b',
        fontSize: 16,
        marginBottom: 20,
    },
    loginButton: {
        backgroundColor: '#8a2be2',
        paddingVertical: 12,
        paddingHorizontal: 32,
        borderRadius: 8,
    },
    loginButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
    profileHeader: {
        alignItems: 'center',
        paddingVertical: 32,
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: '#8a2be2',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
    },
    avatarText: {
        fontSize: 32,
        fontWeight: 'bold',
        color: '#fff',
    },
    nickname: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 4,
    },
    displayTag: {
        fontSize: 14,
        color: '#64748b',
    },
    stats: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        paddingVertical: 24,
        borderBottomWidth: 1,
        borderBottomColor: '#1e1e3f',
    },
    statItem: {
        alignItems: 'center',
    },
    statNumber: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#fff',
    },
    statLabel: {
        fontSize: 12,
        color: '#64748b',
        marginTop: 4,
    },
    menu: {
        padding: 16,
    },
    menuItem: {
        backgroundColor: '#1a1a2e',
        padding: 16,
        borderRadius: 8,
        marginBottom: 8,
    },
    menuText: {
        color: '#fff',
        fontSize: 16,
    },
    logoutItem: {
        marginTop: 16,
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
    },
    logoutText: {
        color: '#ef4444',
        fontSize: 16,
    },
    footer: {
        position: 'absolute',
        bottom: 20,
        left: 0,
        right: 0,
        alignItems: 'center',
    },
    footerText: {
        color: '#475569',
        fontSize: 12,
    },
    footerLink: {
        color: '#8a2be2',
        fontSize: 12,
        marginTop: 4,
    },
});
