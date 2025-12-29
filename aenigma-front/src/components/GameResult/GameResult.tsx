import { useState } from 'react';
import type { GamePlayer, RoleDetail } from '../../api/client';
import './GameResult.css';

interface VoteResult {
    targetId: string;
    targetNickname: string;
    voteCount: number;
}

interface GameResultProps {
    winnerTeam: 'CRIMINAL' | 'SUSPECT' | 'DETECTIVE' | null;
    players: GamePlayer[];
    voteResults: VoteResult[];
    myRole?: RoleDetail;
    scenarioSummary?: string;
    scenarioTitle?: string;
    onLeave: () => void;
}

type ResultView = 'main' | 'roles' | 'case-summary';

/**
 * 게임 결과 화면 메인 컴포넌트
 */
export function GameResult({
    winnerTeam,
    players,
    voteResults,
    scenarioSummary,
    scenarioTitle,
    onLeave,
}: GameResultProps) {
    const [currentView, setCurrentView] = useState<ResultView>('main');

    const getWinnerInfo = () => {
        if (winnerTeam === 'CRIMINAL') {
            return { emoji: '🔪', text: '범인 승리!', subtext: '범인이 끝까지 숨는 데 성공했습니다.' };
        }
        return { emoji: '🎉', text: '시민 승리!', subtext: '범인을 찾아냈습니다!' };
    };

    const winnerInfo = getWinnerInfo();

    if (currentView === 'roles') {
        return (
            <RoleRevealView
                players={players}
                onBack={() => setCurrentView('main')}
            />
        );
    }

    if (currentView === 'case-summary') {
        return (
            <CaseSummaryView
                title={scenarioTitle}
                summary={scenarioSummary}
                onBack={() => setCurrentView('main')}
            />
        );
    }

    return (
        <div className="game-result">
            <div className="game-result__container animate-fadeIn">
                {/* 승리 발표 */}
                <WinnerAnnouncement
                    emoji={winnerInfo.emoji}
                    text={winnerInfo.text}
                    subtext={winnerInfo.subtext}
                />

                {/* 득표 현황 */}
                <VoteTally results={voteResults} />

                {/* 액션 버튼 */}
                <div className="game-result__actions">
                    <button
                        className="btn btn-secondary btn-lg"
                        onClick={() => setCurrentView('roles')}
                    >
                        🎭 역할 공개 보기
                    </button>
                    <button
                        className="btn btn-primary btn-lg btn-ripple"
                        onClick={() => setCurrentView('case-summary')}
                    >
                        📜 사건의 전말 보기
                    </button>
                    <button
                        className="btn btn-ghost"
                        onClick={onLeave}
                    >
                        🚪 나가기
                    </button>
                </div>
            </div>

            {/* 컨페티 효과 */}
            <div className="confetti-container">
                {[...Array(50)].map((_, i) => (
                    <div key={i} className="confetti" style={{
                        left: `${Math.random() * 100}%`,
                        animationDelay: `${Math.random() * 3}s`,
                        backgroundColor: ['#6366f1', '#ec4899', '#10b981', '#f59e0b'][i % 4]
                    }} />
                ))}
            </div>
        </div>
    );
}

/**
 * 승리 팀 발표 컴포넌트
 */
function WinnerAnnouncement({
    emoji,
    text,
    subtext,
}: {
    emoji: string;
    text: string;
    subtext: string;
}) {
    return (
        <div className="winner-announcement">
            <div className="winner-announcement__emoji animate-glow">{emoji}</div>
            <h1 className="winner-announcement__title">{text}</h1>
            <p className="winner-announcement__subtext">{subtext}</p>
        </div>
    );
}

/**
 * 득표 현황 컴포넌트 (누가 누구에게 투표했는지는 비공개)
 */
function VoteTally({ results }: { results: VoteResult[] }) {
    const maxVotes = Math.max(...results.map(r => r.voteCount), 1);
    const sortedResults = [...results].sort((a, b) => b.voteCount - a.voteCount);

    return (
        <div className="vote-tally">
            <h3 className="vote-tally__title">📊 최종 투표 결과</h3>
            <div className="vote-tally__list">
                {sortedResults.map((result, index) => (
                    <div key={result.targetId} className="vote-tally__item">
                        <div className="vote-tally__rank">{index + 1}</div>
                        <div className="vote-tally__name">{result.targetNickname}</div>
                        <div className="vote-tally__bar-container">
                            <div
                                className="vote-tally__bar"
                                style={{ width: `${(result.voteCount / maxVotes) * 100}%` }}
                            />
                        </div>
                        <div className="vote-tally__count">{result.voteCount}표</div>
                    </div>
                ))}
            </div>
        </div>
    );
}

/**
 * 역할 공개 화면
 */
function RoleRevealView({
    players,
    onBack,
}: {
    players: GamePlayer[];
    onBack: () => void;
}) {
    const getRoleEmoji = (role?: string) => {
        switch (role) {
            case 'CRIMINAL': return '🔪';
            case 'DETECTIVE': return '🔍';
            default: return '👤';
        }
    };

    const getRoleLabel = (role?: string) => {
        switch (role) {
            case 'CRIMINAL': return '범인';
            case 'DETECTIVE': return '탐정';
            default: return '용의자';
        }
    };

    return (
        <div className="game-result">
            <div className="game-result__container animate-fadeIn">
                <button className="back-button" onClick={onBack}>
                    ← 돌아가기
                </button>

                <h2 className="section-title">🎭 모든 역할 공개</h2>

                <div className="role-reveal-grid">
                    {players.map((player) => (
                        <div
                            key={player.id}
                            className={`role-card ${player.role === 'CRIMINAL' ? 'role-card--criminal' : ''}`}
                        >
                            <div className="role-card__emoji">{getRoleEmoji(player.role)}</div>
                            <div className="role-card__role">{getRoleLabel(player.role)}</div>
                            <div className="role-card__name">{player.nickname}</div>
                            {!player.isAlive && <div className="role-card__dead">💀 사망</div>}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

/**
 * 사건의 전말 화면
 */
function CaseSummaryView({
    title,
    summary,
    onBack,
}: {
    title?: string;
    summary?: string;
    onBack: () => void;
}) {
    return (
        <div className="game-result">
            <div className="game-result__container game-result__container--wide animate-fadeIn">
                <button className="back-button" onClick={onBack}>
                    ← 돌아가기
                </button>

                <h2 className="section-title">📜 사건의 전말</h2>

                {title && <h3 className="case-summary__title">{title}</h3>}

                <div className="case-summary__content">
                    {summary ? (
                        <div className="case-summary__text">
                            {summary.split('\n').map((paragraph, i) => (
                                <p key={i}>{paragraph}</p>
                            ))}
                        </div>
                    ) : (
                        <div className="case-summary__placeholder">
                            <p>시나리오 정보가 없습니다.</p>
                            <p>GM에게 사건의 전말을 들어보세요!</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default GameResult;
