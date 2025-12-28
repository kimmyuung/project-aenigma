import { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { router, Link } from 'expo-router';
import { useAuth } from '../../contexts/AuthContext';

export default function RegisterScreen() {
    const [nickname, setNickname] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { register } = useAuth();

    const handleRegister = async () => {
        if (!nickname.trim()) {
            Alert.alert('오류', '닉네임을 입력해주세요.');
            return;
        }

        if (nickname.length < 2 || nickname.length > 20) {
            Alert.alert('오류', '닉네임은 2~20자 사이여야 합니다.');
            return;
        }

        setIsLoading(true);
        try {
            await register(nickname);
            router.replace('/(tabs)');
        } catch (error) {
            Alert.alert('회원가입 실패', '이미 사용 중인 닉네임입니다.');
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
                    <Text style={styles.title}>회원가입</Text>
                    <Text style={styles.subtitle}>새 계정을 만들어 게임에 참여하세요!</Text>
                </View>

                <View style={styles.form}>
                    <Text style={styles.label}>닉네임</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="닉네임 입력 (2~20자)..."
                        placeholderTextColor="#64748b"
                        value={nickname}
                        onChangeText={setNickname}
                        maxLength={20}
                    />
                    <Text style={styles.hint}>
                        게임에서 표시될 이름입니다.
                    </Text>

                    <TouchableOpacity
                        style={[styles.button, isLoading && styles.buttonDisabled]}
                        onPress={handleRegister}
                        disabled={isLoading}
                    >
                        <Text style={styles.buttonText}>
                            {isLoading ? '가입 중...' : '가입하기'}
                        </Text>
                    </TouchableOpacity>

                    <View style={styles.loginLink}>
                        <Text style={styles.loginText}>이미 계정이 있으신가요? </Text>
                        <Link href="/(auth)/login" asChild>
                            <TouchableOpacity>
                                <Text style={styles.loginLinkText}>로그인</Text>
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
    },
    subtitle: {
        fontSize: 14,
        color: '#64748b',
        marginTop: 8,
        textAlign: 'center',
    },
    form: {
        gap: 12,
    },
    label: {
        color: '#fff',
        fontSize: 14,
        fontWeight: '600',
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
    hint: {
        color: '#64748b',
        fontSize: 12,
    },
    button: {
        backgroundColor: '#8a2be2',
        paddingVertical: 16,
        borderRadius: 12,
        alignItems: 'center',
        marginTop: 16,
    },
    buttonDisabled: {
        opacity: 0.6,
    },
    buttonText: {
        color: '#fff',
        fontSize: 16,
        fontWeight: '600',
    },
    loginLink: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginTop: 24,
    },
    loginText: {
        color: '#64748b',
    },
    loginLinkText: {
        color: '#8a2be2',
        fontWeight: '600',
    },
});
