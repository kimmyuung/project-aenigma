import { useRef, useEffect } from 'react';
import type { ChatMessage } from './types';
import './ChatPanel.css';

interface ChatPanelProps {
    messages: ChatMessage[];
    userId?: string;
    newMessage: string;
    onNewMessageChange: (message: string) => void;
    onSendMessage: (e: React.FormEvent) => void;
}

export function ChatPanel({
    messages,
    userId,
    newMessage,
    onNewMessageChange,
    onSendMessage,
}: ChatPanelProps) {
    const chatContainerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (chatContainerRef.current) {
            chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
    }, [messages]);

    return (
        <div className="chat-section">
            <div className="chat-messages" ref={chatContainerRef}>
                {messages.map((msg) => (
                    <div
                        key={msg.id}
                        className={`chat-message ${msg.type.toLowerCase()} ${msg.senderId === userId ? 'mine' : ''}`}
                    >
                        {msg.type === 'SYSTEM' ? (
                            <div className="system-message">{msg.content}</div>
                        ) : (
                            <>
                                <span className="sender">{msg.senderNickname}</span>
                                <span className="content">{msg.content}</span>
                            </>
                        )}
                    </div>
                ))}
            </div>

            <form className="chat-input-form" onSubmit={onSendMessage}>
                <input
                    type="text"
                    className="chat-input"
                    placeholder="메시지를 입력하세요..."
                    value={newMessage}
                    onChange={(e) => onNewMessageChange(e.target.value)}
                />
                <button type="submit" className="btn btn-primary">
                    전송
                </button>
            </form>
        </div>
    );
}
