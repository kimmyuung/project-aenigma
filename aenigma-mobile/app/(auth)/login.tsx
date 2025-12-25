import React, { useState } from 'react';
import {
    StyleSheet,
    View,
    Text,
    TextInput,
    TouchableOpacity,
    ActivityIndicator,
    KeyboardAvoidingView,
    Platform,
    Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../../contexts';

export default function LoginScreen() {
    const [nickname, setNickname] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const router = useRouter();

    const handleLogin = async () => {
        if (nickname.trim().length < 2) {
            Alert.alert('오류', '닉네임은 2자 이상이어야 합니다.');
            return;
        }

        setIsLoading(true);
        try {
            await login(nickname.trim());
            router.replace('/(tabs)');
        } catch (error: any) {
            Alert.alert('로그인 실패', error.message || '다시 시도해주세요.');
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
                {/* Logo / Title */}
                <View style={styles.header}>
                    <Text style={styles.title}>🔍 AENIGMA</Text>
                    <Text style={styles.subtitle}>추리 게임의 세계로</Text>
                </View>

                {/* Login Form */}
                <View style={styles.form}>
                    <Text style={styles.label}>닉네임</Text>
                    <TextInput
                        style={styles.input}
                        placeholder="닉네임을 입력하세요"
                        placeholderTextColor="#888"
                        value={nickname}
                        onChangeText={setNickname}
                        maxLength={30}
                        autoCapitalize="none"
                        autoCorrect={false}
                    />

                    <TouchableOpacity
                        style={[styles.button, isLoading && styles.buttonDisabled]}
                        onPress={handleLogin}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.buttonText}>게스트로 시작하기</Text>
                        )}
                    </TouchableOpacity>
                </View>

                {/* Info */}
                <Text style={styles.info}>
                    게스트 계정으로 바로 게임을 시작할 수 있습니다.
                </Text>
            </View>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#1a1a2e',
    },
    content: {
        flex: 1,
        justifyContent: 'center',
        paddingHorizontal: 32,
    },
    header: {
        alignItems: 'center',
        marginBottom: 48,
    },
    title: {
        fontSize: 42,
        fontWeight: 'bold',
        color: '#fff',
        marginBottom: 8,
    },
    subtitle: {
        fontSize: 18,
        color: '#a0a0a0',
    },
    form: {
        marginBottom: 24,
    },
    label: {
        fontSize: 14,
        color: '#a0a0a0',
        marginBottom: 8,
    },
    input: {
        backgroundColor: '#16213e',
        borderRadius: 12,
        padding: 16,
        fontSize: 16,
        color: '#fff',
        marginBottom: 16,
        borderWidth: 1,
        borderColor: '#0f3460',
    },
    button: {
        backgroundColor: '#e94560',
        borderRadius: 12,
        padding: 16,
        alignItems: 'center',
    },
    buttonDisabled: {
        opacity: 0.7,
    },
    buttonText: {
        color: '#fff',
        fontSize: 18,
        fontWeight: '600',
    },
    info: {
        textAlign: 'center',
        color: '#666',
        fontSize: 14,
    },
});
