import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';

export type ConnectionState = 'connected' | 'connecting' | 'disconnected' | 'reconnecting';

interface ConnectionIndicatorProps {
    state: ConnectionState;
    retryCount?: number;
    maxRetries?: number;
    onReconnect?: () => void;
}

export function ConnectionIndicator({
    state,
    retryCount = 0,
    maxRetries = 5,
    onReconnect,
}: ConnectionIndicatorProps) {
    const getStateConfig = () => {
        switch (state) {
            case 'connected':
                return {
                    color: '#22c55e',
                    text: '연결됨',
                    icon: '🟢',
                };
            case 'connecting':
                return {
                    color: '#f59e0b',
                    text: '연결 중...',
                    icon: null,
                    showSpinner: true,
                };
            case 'reconnecting':
                return {
                    color: '#f59e0b',
                    text: `재연결 중... (${retryCount}/${maxRetries})`,
                    icon: null,
                    showSpinner: true,
                };
            case 'disconnected':
                return {
                    color: '#ef4444',
                    text: '연결 끊김',
                    icon: '🔴',
                };
            default:
                return {
                    color: '#64748b',
                    text: '알 수 없음',
                    icon: '⚪',
                };
        }
    };

    const config = getStateConfig();

    return (
        <View style={styles.container}>
            <View style={styles.indicator}>
                {config.showSpinner ? (
                    <ActivityIndicator size="small" color={config.color} />
                ) : (
                    <View style={[styles.dot, { backgroundColor: config.color }]} />
                )}
                <Text style={[styles.text, { color: config.color }]}>
                    {config.text}
                </Text>
            </View>

            {state === 'disconnected' && onReconnect && (
                <TouchableOpacity style={styles.reconnectButton} onPress={onReconnect}>
                    <Text style={styles.reconnectText}>🔄 재연결</Text>
                </TouchableOpacity>
            )}
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 8,
        paddingHorizontal: 16,
        backgroundColor: 'rgba(0, 0, 0, 0.3)',
        gap: 12,
    },
    indicator: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    dot: {
        width: 10,
        height: 10,
        borderRadius: 5,
    },
    text: {
        fontSize: 12,
        fontWeight: '500',
    },
    reconnectButton: {
        backgroundColor: '#3b82f6',
        paddingVertical: 4,
        paddingHorizontal: 12,
        borderRadius: 12,
    },
    reconnectText: {
        color: '#fff',
        fontSize: 12,
        fontWeight: '600',
    },
});

export default ConnectionIndicator;
