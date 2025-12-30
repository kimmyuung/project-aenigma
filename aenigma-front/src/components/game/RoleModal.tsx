import type { RoleDetail } from './types';
import { getRoleEmoji } from './types';
import './RoleModal.css';

interface AlibiEntry {
    time: string;
    location: string;
    activity: string;
    witnesses?: string[];
}

interface RoleModalProps {
    roleDetail: RoleDetail;
    onClose: () => void;
}

export function RoleModal({ roleDetail, onClose }: RoleModalProps) {
    const parseAlibi = (): AlibiEntry[] | null => {
        if (!roleDetail.alibi) return null;
        try {
            return JSON.parse(roleDetail.alibi) as AlibiEntry[];
        } catch {
            return null;
        }
    };

    const alibiEntries = parseAlibi();

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content role-modal" onClick={(e) => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose}>×</button>
                <div className="role-header">
                    <span className="role-emoji-lg">{getRoleEmoji(roleDetail.roleType)}</span>
                    <h2>{roleDetail.roleName || roleDetail.roleType}</h2>
                </div>
                {roleDetail.description && (
                    <div className="role-section">
                        <h4>📖 설명</h4>
                        <p>{roleDetail.description}</p>
                    </div>
                )}
                {roleDetail.objective && (
                    <div className="role-section">
                        <h4>🎯 목표</h4>
                        <p>{roleDetail.objective}</p>
                    </div>
                )}
                {roleDetail.secretInfo && (
                    <div className="role-section secret">
                        <h4>🔐 비밀 정보</h4>
                        <p>{roleDetail.secretInfo}</p>
                    </div>
                )}
                {alibiEntries && alibiEntries.length > 0 && (
                    <div className="role-section alibi">
                        <h4>📅 사건 당일 알리바이</h4>
                        <div className="alibi-timeline">
                            {alibiEntries.map((entry, idx) => (
                                <div key={idx} className="alibi-entry">
                                    <span className="alibi-time">{entry.time}</span>
                                    <div className="alibi-content">
                                        <span className="alibi-location">📍 {entry.location}</span>
                                        <span className="alibi-activity">{entry.activity}</span>
                                        {entry.witnesses && entry.witnesses.length > 0 && (
                                            <span className="alibi-witnesses">👁️ 목격자: {entry.witnesses.join(', ')}</span>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
