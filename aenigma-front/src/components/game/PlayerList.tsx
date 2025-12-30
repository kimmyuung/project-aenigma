import type { GamePlayerInfo, GamePhase } from './types';
import './PlayerList.css';

interface PlayerListProps {
    players: GamePlayerInfo[];
    phase: GamePhase;
    selectedPlayer: string | null;
    votedPlayer: string | null;
    onPlayerSelect: (playerId: string) => void;
}

export function PlayerList({
    players,
    phase,
    selectedPlayer,
    votedPlayer,
    onPlayerSelect,
}: PlayerListProps) {
    const canVote = phase === 'FINAL_VOTE';

    return (
        <aside className="players-panel">
            <h3>참가자</h3>
            <div className="players-list">
                {players.map((player) => (
                    <div
                        key={player.id}
                        className={`player-item ${!player.isAlive ? 'eliminated' : ''} ${selectedPlayer === player.id ? 'selected' : ''}`}
                        onClick={() => player.isAlive && canVote && onPlayerSelect(player.id)}
                    >
                        <div className="player-avatar">
                            {player.nickname.charAt(0).toUpperCase()}
                        </div>
                        <span className="player-name">{player.nickname}</span>
                        {!player.isAlive && <span className="eliminated-badge">💀</span>}
                        {votedPlayer === player.id && <span className="voted-badge">✓</span>}
                    </div>
                ))}
            </div>
        </aside>
    );
}
