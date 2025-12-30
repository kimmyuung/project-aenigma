import type { RoleDetail } from '../../api/client';

export type GamePhase = 'INTRO' | 'LOBBY' | 'INVESTIGATION' | 'FINAL_VOTE' | 'CONCLUSION' | 'FINISHED';

export interface GameState {
    id: string;
    phase: GamePhase;
    round: number;
    maxRounds: number;
    players: GamePlayerInfo[];
    myRole?: string;
}

export interface GamePlayerInfo {
    id: string;
    playerId: string;
    userId: string;
    nickname: string;
    displayTag: string;
    role?: string;
    isAlive: boolean;
}

export interface ChatMessage {
    id: string;
    gameId: string;
    senderId: string;
    senderNickname: string;
    content: string;
    type: 'PUBLIC' | 'WHISPER' | 'CRIMINAL' | 'SYSTEM';
    timestamp: string;
    receiverId?: string;
    receiverNickname?: string;
}

// Helper functions
export const getPhaseTitle = (phase: GamePhase): string => {
    switch (phase) {
        case 'INTRO': return '🎭 도입';
        case 'LOBBY': return '📜 역할 숙지';
        case 'INVESTIGATION': return '🔍 조사 시간';
        case 'FINAL_VOTE': return '⚖️ 최종 투표';
        case 'CONCLUSION': return '🎬 결과 발표';
        case 'FINISHED': return '🏆 게임 종료';
        default: return phase;
    }
};

export const getRoleEmoji = (role?: string): string => {
    switch (role) {
        case 'CRIMINAL': return '🔪';
        case 'DETECTIVE': return '🔍';
        case 'CITIZEN': return '👤';
        default: return '❓';
    }
};

export const getClueTypeClass = (clueType: string): string => {
    switch (clueType) {
        case 'PUBLIC': return 'public';
        case 'PERSONAL': return 'private';
        case 'HIDDEN': return 'secret';
        default: return '';
    }
};

export const getClueTypeLabel = (clueType: string): string => {
    switch (clueType) {
        case 'PUBLIC': return '🔍 공개 단서';
        case 'PERSONAL': return '🎭 개인 단서';
        case 'HIDDEN': return '🔒 비밀 단서';
        default: return '📋 단서';
    }
};

export type { RoleDetail };
