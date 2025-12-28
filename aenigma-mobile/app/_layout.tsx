import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { AuthProvider } from '../contexts/AuthContext';

export default function RootLayout() {
    return (
        <AuthProvider>
            <StatusBar style="light" />
            <Stack
                screenOptions={{
                    headerStyle: {
                        backgroundColor: '#0f0f1e',
                    },
                    headerTintColor: '#fff',
                    contentStyle: {
                        backgroundColor: '#0f0f1e',
                    },
                }}
            >
                <Stack.Screen
                    name="(tabs)"
                    options={{ headerShown: false }}
                />
                <Stack.Screen
                    name="(auth)"
                    options={{ headerShown: false }}
                />
                <Stack.Screen
                    name="game/[gameId]"
                    options={{
                        title: '게임',
                        headerBackTitle: '나가기',
                    }}
                />
                <Stack.Screen
                    name="room/[roomId]"
                    options={{
                        title: '대기실',
                        headerBackTitle: '나가기',
                    }}
                />
            </Stack>
        </AuthProvider>
    );
}
