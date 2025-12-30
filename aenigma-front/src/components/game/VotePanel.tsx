import './VotePanel.css';

interface VotePanelProps {
    selectedPlayer: string | null;
    votedPlayer: string | null;
    isVoting: boolean;
    voteError: string | null;
    onVote: () => void;
}

export function VotePanel({
    selectedPlayer,
    votedPlayer,
    isVoting,
    voteError,
    onVote,
}: VotePanelProps) {
    return (
        <div className="vote-section">
            <h3>⚖️ 최종 투표</h3>
            <p>범인이라고 생각하는 사람을 선택하세요.</p>
            {voteError && <p className="error-message">{voteError}</p>}
            <div className="vote-actions">
                <button
                    className="btn btn-primary btn-lg"
                    onClick={onVote}
                    disabled={!selectedPlayer || !!votedPlayer || isVoting}
                >
                    {isVoting ? '투표 중...' : votedPlayer ? '투표 완료' : '투표하기'}
                </button>
            </div>
        </div>
    );
}
