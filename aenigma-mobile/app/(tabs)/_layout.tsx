import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

export default function TabLayout() {
    return (
        <Tabs
            screenOptions={{
                tabBarActiveTintColor: '#8a2be2',
                tabBarInactiveTintColor: '#64748b',
                tabBarStyle: {
                    backgroundColor: '#0f0f1e',
                    borderTopColor: '#1e1e3f',
                    height: 60,
                    paddingBottom: 8,
                },
                headerStyle: {
                    backgroundColor: '#0f0f1e',
                },
                headerTintColor: '#fff',
            }}
        >
            <Tabs.Screen
                name="index"
                options={{
                    title: '홈',
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="home" size={size} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="rooms"
                options={{
                    title: '방 목록',
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="people" size={size} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="profile"
                options={{
                    title: '프로필',
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="person" size={size} color={color} />
                    ),
                }}
            />
        </Tabs>
    );
}
