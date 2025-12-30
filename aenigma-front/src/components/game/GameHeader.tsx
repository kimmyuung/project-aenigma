import type { GamePhase } from './types';
import { getPhaseTitle, getRoleEmoji } from './types';
import './GameHeader.css';

interface GameHeaderProps {
    phase: GamePhase;
    round: number;
    maxRounds: number;
    myRole?: string;
    wsStatus: 'connecting' | 'connected' | 'disconnected';
    onRoleClick: () => void;
}

export function GameHeader({
    phase,
    round,
    maxRounds,
    myRole,
    wsStatus,
    onRoleClick,
}: GameHeaderProps) {
    return (
        <header className="game-header">
            <div className="game-info">
                <span className="phase-badge">{getPhaseTitle(phase)}</span>
                {phase === 'INVESTIGATION' && (
                    <span className="round-badge">라운드 {round} / {maxRounds}</span>
                )}
                <span className={`ws-status ${wsStatus}`}>
                    {wsStatus === 'connected' ? '🟢' : wsStatus === 'connecting' ? '🟡' : '🔴'}
                </span>
            </div>
            <div className="my-role" onClick={onRoleClick} style={{ cursor: 'pointer' }}>
                <span className="role-emoji">{getRoleEmoji(myRole)}</span>
                <span className="role-name">{myRole || '역할 미정'}</span>
                <span className="role-hint">ℹ️</span>
            </div>
        </header>
    );
}
