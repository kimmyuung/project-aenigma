import { type ReactNode } from 'react';
import './MobileNav.css';

export type MobileTabType = 'players' | 'chat' | 'clues';

interface MobileNavProps {
    activeTab: MobileTabType;
    onTabChange: (tab: MobileTabType) => void;
    unreadMessages?: number;
}

interface TabConfig {
    id: MobileTabType;
    icon: string;
    label: string;
}

const tabs: TabConfig[] = [
    { id: 'players', icon: '👥', label: '플레이어' },
    { id: 'chat', icon: '💬', label: '채팅' },
    { id: 'clues', icon: '🔍', label: '단서' },
];

export function MobileNav({ activeTab, onTabChange, unreadMessages = 0 }: MobileNavProps) {
    return (
        <nav className="mobile-nav">
            {tabs.map((tab) => (
                <button
                    key={tab.id}
                    className={`mobile-nav-tab ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => onTabChange(tab.id)}
                    type="button"
                >
                    <span className="tab-icon">
                        {tab.icon}
                        {tab.id === 'chat' && unreadMessages > 0 && (
                            <span className="unread-badge">
                                {unreadMessages > 9 ? '9+' : unreadMessages}
                            </span>
                        )}
                    </span>
                    <span className="tab-label">{tab.label}</span>
                </button>
            ))}
        </nav>
    );
}

// 모바일 패널 래퍼
interface MobilePanelProps {
    isVisible: boolean;
    position?: 'left' | 'right' | 'bottom';
    children: ReactNode;
}

export function MobilePanel({ isVisible, position = 'bottom', children }: MobilePanelProps) {
    if (!isVisible) return null;

    const animationClass = position === 'left'
        ? 'animate-slideInLeft'
        : position === 'right'
            ? 'animate-slideInRight'
            : 'animate-slideInUp';

    return (
        <div className={`mobile-panel mobile-panel-${position} ${animationClass}`}>
            {children}
        </div>
    );
}
