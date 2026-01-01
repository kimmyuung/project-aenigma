import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CluePanel } from '../components/game/CluePanel';
import type { Clue } from '../api/client';

// Mock CSS
vi.mock('../components/game/CluePanel.css', () => ({}));

// 테스트용 Mock Data
const mockClues: Clue[] = [
    {
        id: 'clue-1',
        title: '혈흔',
        content: '현장에서 발견된 혈흔입니다.',
        clueType: 'PUBLIC',
        importance: 5,
        isDiscovered: true,
        discoveredByNickname: '탐정',
    },
    {
        id: 'clue-2',
        title: '익명의 편지',
        content: '협박 편지가 발견되었습니다.',
        clueType: 'PERSONAL',
        importance: 3,
        isDiscovered: true,
    },
    {
        id: 'clue-3',
        title: '숨겨진 단서',
        content: '아직 발견되지 않은 단서',
        clueType: 'HIDDEN',
        importance: 4,
        isDiscovered: false,
    },
];

const mockRoleDetail = {
    playerId: 'player-1',
    nickname: '테스터',
    displayTag: '#0001',
    roleType: 'SUSPECT',
    roleName: '탐정',
    description: '범인을 찾아내야 합니다.',
    objective: '모든 단서를 수집하고 범인을 지목하세요.',
    isAlive: true,
};

describe('CluePanel', () => {
    const defaultProps = {
        clues: mockClues,
        myRole: 'SUSPECT',
        roleDetail: mockRoleDetail,
        onRoleCardClick: vi.fn(),
    };

    describe('역할 카드', () => {
        it('내 역할 정보가 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('내 역할')).toBeInTheDocument();
            expect(screen.getByText('탐정')).toBeInTheDocument();
        });

        it('역할 목표가 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('모든 단서를 수집하고 범인을 지목하세요.')).toBeInTheDocument();
        });

        it('역할 카드 클릭 시 콜백 호출', () => {
            const onRoleCardClick = vi.fn();
            render(<CluePanel {...defaultProps} onRoleCardClick={onRoleCardClick} />);

            const roleCard = screen.getByText('내 역할').closest('.role-card');
            fireEvent.click(roleCard!);

            expect(onRoleCardClick).toHaveBeenCalled();
        });

        it('roleDetail 없을 때 기본 역할명 표시', () => {
            render(<CluePanel {...defaultProps} roleDetail={null} myRole="CRIMINAL" />);

            expect(screen.getByText('CRIMINAL')).toBeInTheDocument();
        });
    });

    describe('단서 목록', () => {
        it('단서 섹션 헤더가 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('📋 단서')).toBeInTheDocument();
        });

        it('발견된 단서 제목이 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('혈흔')).toBeInTheDocument();
            expect(screen.getByText('익명의 편지')).toBeInTheDocument();
        });

        it('발견된 단서 내용이 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('현장에서 발견된 혈흔입니다.')).toBeInTheDocument();
        });

        it('미발견 단서는 ??? 로 표시됨', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('???')).toBeInTheDocument();
            expect(screen.getByText('아직 발견되지 않은 단서입니다.')).toBeInTheDocument();
        });

        it('발견된 단서에 ✅ 아이콘 표시', () => {
            render(<CluePanel {...defaultProps} />);

            const discoveredIcons = screen.getAllByText('✅');
            expect(discoveredIcons.length).toBeGreaterThan(0);
        });

        it('미발견 단서에 🔒 아이콘 표시', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('🔒')).toBeInTheDocument();
        });
    });

    describe('단서 타입별 표시', () => {
        it('공개 단서 타입 라벨 표시', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('🔍 공개 단서')).toBeInTheDocument();
        });

        it('개인 단서 타입 라벨 표시', () => {
            render(<CluePanel {...defaultProps} />);

            expect(screen.getByText('🎭 개인 단서')).toBeInTheDocument();
        });
    });

    describe('빈 단서 목록', () => {
        it('단서가 없을 때 기본 UI 표시', () => {
            render(<CluePanel {...defaultProps} clues={[]} />);

            expect(screen.getByText('비밀 정보')).toBeInTheDocument();
            expect(screen.getByText('당신만 아는 비밀 정보입니다.')).toBeInTheDocument();
            expect(screen.getByText('사건 현장 증거')).toBeInTheDocument();
        });
    });
});
