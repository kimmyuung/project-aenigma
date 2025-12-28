import { Stack } from 'expo-router';

export default function AuthLayout() {
    return (
        <Stack
            screenOptions={{
                headerStyle: {
                    backgroundColor: '#0f0f1e',
                },
                headerTintColor: '#fff',
                headerBackTitle: '뒤로',
            }}
        >
            <Stack.Screen
                name="login"
                options={{
                    title: '로그인',
                }}
            />
            <Stack.Screen
                name="register"
                options={{
                    title: '회원가입',
                }}
            />
        </Stack>
    );
}
