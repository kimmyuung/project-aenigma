import React, { useRef, useEffect } from 'react';
import {
    View,
    Text,
    StyleSheet,
    ScrollView,
    TextInput,
    TouchableOpacity,
    KeyboardAvoidingView,
    Platform,
} from 'react-native';

export interface ChatMessage {
    id: string;
    gameId?: string;
    roomId?: string;
    senderId: string;
    senderNickname: string;
    content: string;
    type: 'PUBLIC' | 'WHISPER' | 'SYSTEM' | 'GM';
    timestamp: string;
}

interface ChatBoxProps {
    messages: ChatMessage[];
    currentUserId: string;
    newMessage: string;
    onMessageChange: (text: string) => void;
    onSendMessage: () => void;
    isConnected?: boolean;
    placeholder?: string;
}

export function ChatBox({
    messages,
    currentUserId,
    newMessage,
    onMessageChange,
    onSendMessage,
    isConnected = true,
    placeholder = '메시지 입력...',
}: ChatBoxProps) {
    const scrollViewRef = useRef<ScrollView>(null);

    useEffect(() => {
        // 새 메시지 시 스크롤 하단으로 이동
        scrollViewRef.current?.scrollToEnd({ animated: true });
    }, [messages]);

    const getMessageStyle = (message: ChatMessage) => {
        if (message.type === 'SYSTEM') {
            return styles.systemMessage;
        }
        if (message.type === 'GM') {
            return styles.gmMessage;
        }
        if (message.type === 'WHISPER') {
            return styles.whisperMessage;
        }
        if (message.senderId === currentUserId) {
            return styles.myMessage;
        }
        return styles.otherMessage;
    };

    const getMessageTextStyle = (message: ChatMessage) => {
        if (message.type === 'SYSTEM') {
            return styles.systemMessageText;
        }
        if (message.type === 'GM') {
            return styles.gmMessageText;
        }
        if (message.type === 'WHISPER') {
            return styles.whisperMessageText;
        }
        return styles.messageText;
    };

    const formatTime = (timestamp: string) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <View style={styles.container}>
            <ScrollView
                ref={scrollViewRef}
                style={styles.messageList}
                contentContainerStyle={styles.messageListContent}
            >
                {messages.map((msg) => (
                    <View
                        key={msg.id}
                        style={[styles.messageItem, getMessageStyle(msg)]}
                    >
                        {msg.type === 'SYSTEM' ? (
                            <Text style={styles.systemMessageText}>{msg.content}</Text>
                        ) : (
                            <>
                                {msg.senderId !== currentUserId && (
                                    <Text style={styles.senderName}>
                                        {msg.type === 'GM' && '🎮 '}
                                        {msg.type === 'WHISPER' && '🤫 '}
                                        {msg.senderNickname}
                                    </Text>
                                )}
                                <Text style={getMessageTextStyle(msg)}>{msg.content}</Text>
                                <Text style={styles.timestamp}>{formatTime(msg.timestamp)}</Text>
                            </>
                        )}
                    </View>
                ))}
            </ScrollView>

            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            >
                <View style={styles.inputContainer}>
                    <TextInput
                        style={[styles.input, !isConnected && styles.inputDisabled]}
                        placeholder={placeholder}
                        placeholderTextColor="#64748b"
                        value={newMessage}
                        onChangeText={onMessageChange}
                        onSubmitEditing={onSendMessage}
                        returnKeyType="send"
                        editable={isConnected}
                    />
                    <TouchableOpacity
                        style={[styles.sendButton, !isConnected && styles.sendButtonDisabled]}
                        onPress={onSendMessage}
                        disabled={!isConnected || !newMessage.trim()}
                    >
                        <Text style={styles.sendButtonText}>전송</Text>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0f0f1e',
    },
    messageList: {
        flex: 1,
    },
    messageListContent: {
        padding: 12,
        gap: 8,
    },
    messageItem: {
        borderRadius: 12,
        padding: 10,
        maxWidth: '80%',
    },
    otherMessage: {
        alignSelf: 'flex-start',
        backgroundColor: '#1a1a2e',
    },
    myMessage: {
        alignSelf: 'flex-end',
        backgroundColor: '#8a2be2',
    },
    systemMessage: {
        alignSelf: 'center',
        backgroundColor: 'transparent',
        maxWidth: '100%',
    },
    gmMessage: {
        alignSelf: 'flex-start',
        backgroundColor: 'rgba(249, 115, 22, 0.2)',
        borderLeftWidth: 3,
        borderLeftColor: '#f97316',
    },
    whisperMessage: {
        alignSelf: 'flex-start',
        backgroundColor: 'rgba(236, 72, 153, 0.2)',
        borderLeftWidth: 3,
        borderLeftColor: '#ec4899',
    },
    senderName: {
        color: '#64748b',
        fontSize: 11,
        marginBottom: 2,
    },
    messageText: {
        color: '#fff',
        fontSize: 14,
    },
    systemMessageText: {
        color: '#64748b',
        fontStyle: 'italic',
        textAlign: 'center',
        fontSize: 12,
    },
    gmMessageText: {
        color: '#f97316',
        fontSize: 14,
    },
    whisperMessageText: {
        color: '#ec4899',
        fontSize: 14,
    },
    timestamp: {
        color: 'rgba(255, 255, 255, 0.4)',
        fontSize: 10,
        marginTop: 4,
        alignSelf: 'flex-end',
    },
    inputContainer: {
        flexDirection: 'row',
        padding: 12,
        gap: 8,
        borderTopWidth: 1,
        borderTopColor: '#1e1e3f',
    },
    input: {
        flex: 1,
        backgroundColor: '#1a1a2e',
        borderRadius: 20,
        paddingHorizontal: 16,
        paddingVertical: 10,
        color: '#fff',
        fontSize: 14,
    },
    inputDisabled: {
        opacity: 0.5,
    },
    sendButton: {
        backgroundColor: '#8a2be2',
        borderRadius: 20,
        paddingHorizontal: 20,
        justifyContent: 'center',
    },
    sendButtonDisabled: {
        opacity: 0.5,
    },
    sendButtonText: {
        color: '#fff',
        fontWeight: '600',
    },
});

export default ChatBox;
