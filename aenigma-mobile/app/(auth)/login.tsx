import { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { router, Link } from 'expo-router';
import { useAuth } from '../../contexts/AuthContext';

export default function LoginScreen() {
    const [username, setUsername] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();

    const handleLogin = async () => {
        if (!username.trim()) {
            Alert.alert('오류', '사용자명을 입력해주세요.');
            return;
        }

        setIsLoading(true);
        try {
            await login(username);
            router.replace('/(tabs)');
        } catch (error) {
            Alert.alert('로그인 실패', '존재하지 않는 사용자입니다. 회원가입을 해주세요.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <KeyboardAvoidingView
            style={styles.container}
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
            <View style={styles.content}>
                <View style={styles.header}>
                    <Text style={styles.logo}>🎭</Text>
                    <Text style={styles.title}>AENIGMA</Text>
                    <Text style={styles.subtitle}>온라인 머더미스터리 게임</Text>
                </View>

                <View style={styles.form}>
                    <Text style={styles.label}>사용자명</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="사용자명 입력..."
                        placeholderTextColor="#64748b"
                        value={username}
                        onChangeText={setUsername}
                        autoCapitalize="none"
                        autoCorrect={false}
                    />

                    <TouchableOpacity
                        style={[styles.button, isLoading && styles.buttonDisabled]}
                        onPress={handleLogin}
                        disabled={isLoading}
                    >
                        <Text style={styles.buttonText}>
                            {isLoading ? '로그인 중...' : '로그인'}
                        </Text>
                    </TouchableOpacity>

                    <View style={styles.registerLink}>
                        <Text style={styles.registerText}>계정이 없으신가요? </Text>
                        <Link href="/(auth)/register" asChild>
                            <TouchableOpacity>
                                <Text style={styles.registerLinkText}>회원가입</Text>
                            </TouchableOpacity>
                        </Link>
                    </View>
                </View>
            </View>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    content: {
        flex: 1,
        justifyContent: 'center',
        padding: 24,
    },
    header: {
        alignItems: 'center',
        marginBottom: 48,
    },
    logo: {
        fontSize: 64,
        marginBottom: 16,
    },
    title: {
        fontSize: 28,
        fontWeight: 'bold',
        color: '#fff',
        letterSpacing: 4,
    },
    subtitle: {
        fontSize: 14,
        color: '#64748b',
        marginTop: 8,
    },
    form: {
        gap: 16,
    },
    label: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '600',
        marginBottom: 4,
    },
    input: {
        backgroundColor: '#1a1a2e',
        borderWidth: 1,
        borderColor: '#334155',
        borderRadius: 12,
        padding: 16,
        color: '#fff',
        fontSize: 16,
    },
    button: {
        backgroundColor: '#8a2be2',
        paddingVertical: 16,
        borderRadius: 12,
        alignItems: 'center',
        marginTop: 8,
    },
    buttonDisabled: {
        opacity: 0.6,
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    registerLink: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginTop: 24,
    },
    registerText: {
        color: '#64748b',
    },
    registerLinkText: {
        color: '#8a2be2',
        fontWeight: '600',
    },
});
