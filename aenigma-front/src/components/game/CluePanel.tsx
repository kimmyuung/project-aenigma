import type { Clue } from '../../api/client';
import type { RoleDetail } from './types';
import { getClueTypeClass, getClueTypeLabel, getRoleEmoji } from './types';
import './CluePanel.css';

interface CluePanelProps {
    clues: Clue[];
    myRole?: string;
    roleDetail: RoleDetail | null;
    onRoleCardClick: () => void;
}

export function CluePanel({
    clues,
    myRole,
    roleDetail,
    onRoleCardClick,
}: CluePanelProps) {
    return (
        <aside className="clues-panel">
            {/* 내 역할 카드 */}
            <div className="role-card" onClick={onRoleCardClick}>
                <div className="role-card-header">
                    <span className="role-emoji-lg">{getRoleEmoji(myRole)}</span>
                    <div className="role-card-info">
                        <span className="role-label">내 역할</span>
                        <span className="role-name-lg">{roleDetail?.roleName || myRole || '미정'}</span>
                    </div>
                    <span className="role-arrow">›</span>
                </div>
                {roleDetail?.objective && (
                    <p className="role-objective">{roleDetail.objective}</p>
                )}
            </div>

            <h3>📋 단서</h3>
            <div className="clues-list">
                {clues.length > 0 ? (
                    clues.map((clue) => (
                        <div
                            key={clue.id}
                            className={`clue-card ${getClueTypeClass(clue.clueType)} ${clue.isDiscovered ? 'discovered' : 'locked'}`}
                        >
                            <span className="clue-icon">
                                {clue.isDiscovered ? '✅' : '🔒'}
                            </span>
                            <span className="clue-type">{getClueTypeLabel(clue.clueType)}</span>
                            <div className="clue-title">
                                {clue.isDiscovered ? clue.title : '???'}
                            </div>
                            <p className="clue-description">
                                {clue.isDiscovered ? clue.content : '아직 발견되지 않은 단서입니다.'}
                            </p>
                        </div>
                    ))
                ) : (
                    <>
                        <div className="clue-card private">
                            <span className="clue-icon">🔐</span>
                            <span className="clue-type">🎭 개인 단서</span>
                            <div className="clue-title">비밀 정보</div>
                            <p className="clue-description">
                                당신만 아는 비밀 정보입니다.
                            </p>
                        </div>
                        <div className="clue-card public discovered">
                            <span className="clue-icon">✅</span>
                            <span className="clue-type">🔍 공개 단서</span>
                            <div className="clue-title">사건 현장 증거</div>
                            <p className="clue-description">
                                사건 현장에서 발견된 증거입니다.
                            </p>
                        </div>
                    </>
                )}
            </div>
        </aside>
    );
}
