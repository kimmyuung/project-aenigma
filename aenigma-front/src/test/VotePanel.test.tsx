import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { VotePanel } from '../components/game/VotePanel';

// Mock CSS
vi.mock('../components/game/VotePanel.css', () => ({}));

describe('VotePanel', () => {
    const defaultProps = {
        selectedPlayer: null,
        votedPlayer: null,
        isVoting: false,
        voteError: null,
        onVote: vi.fn(),
    };

    describe('기본 렌더링', () => {
        it('투표 섹션 헤더가 표시됨', () => {
            render(<VotePanel {...defaultProps} />);

            expect(screen.getByText('⚖️ 최종 투표')).toBeInTheDocument();
        });

        it('투표 안내 메시지가 표시됨', () => {
            render(<VotePanel {...defaultProps} />);

            expect(screen.getByText('범인이라고 생각하는 사람을 선택하세요.')).toBeInTheDocument();
        });

        it('투표하기 버튼이 표시됨', () => {
            render(<VotePanel {...defaultProps} />);

            expect(screen.getByRole('button', { name: '투표하기' })).toBeInTheDocument();
        });
    });

    describe('버튼 비활성화 상태', () => {
        it('플레이어 미선택 시 버튼 비활성화', () => {
            render(<VotePanel {...defaultProps} />);

            const button = screen.getByRole('button', { name: '투표하기' });
            expect(button).toBeDisabled();
        });

        it('이미 투표 완료 시 버튼 비활성화', () => {
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" votedPlayer="player-1" />);

            const button = screen.getByRole('button', { name: '투표 완료' });
            expect(button).toBeDisabled();
        });

        it('투표 중일 때 버튼 비활성화', () => {
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" isVoting={true} />);

            const button = screen.getByRole('button', { name: '투표 중...' });
            expect(button).toBeDisabled();
        });
    });

    describe('버튼 활성화 상태', () => {
        it('플레이어 선택 시 버튼 활성화', () => {
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" />);

            const button = screen.getByRole('button', { name: '투표하기' });
            expect(button).not.toBeDisabled();
        });
    });

    describe('버튼 텍스트 상태', () => {
        it('기본 상태: 투표하기', () => {
            render(<VotePanel {...defaultProps} />);

            expect(screen.getByRole('button', { name: '투표하기' })).toBeInTheDocument();
        });

        it('투표 중: 투표 중...', () => {
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" isVoting={true} />);

            expect(screen.getByRole('button', { name: '투표 중...' })).toBeInTheDocument();
        });

        it('투표 완료: 투표 완료', () => {
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" votedPlayer="player-1" />);

            expect(screen.getByRole('button', { name: '투표 완료' })).toBeInTheDocument();
        });
    });

    describe('에러 메시지', () => {
        it('에러가 없으면 에러 메시지 미표시', () => {
            render(<VotePanel {...defaultProps} />);

            // error-message 클래스를 가진 요소가 없는지 확인
            const errorElement = document.querySelector('.error-message');
            expect(errorElement).toBeNull();
        });

        it('에러가 있으면 에러 메시지 표시', () => {
            render(<VotePanel {...defaultProps} voteError="투표에 실패했습니다." />);

            expect(screen.getByText('투표에 실패했습니다.')).toBeInTheDocument();
        });
    });

    describe('투표 콜백', () => {
        it('투표 버튼 클릭 시 onVote 호출', () => {
            const onVote = vi.fn();
            render(<VotePanel {...defaultProps} selectedPlayer="player-1" onVote={onVote} />);

            const button = screen.getByRole('button', { name: '투표하기' });
            fireEvent.click(button);

            expect(onVote).toHaveBeenCalled();
        });

        it('비활성화 상태에서 클릭 시 onVote 미호출', () => {
            const onVote = vi.fn();
            render(<VotePanel {...defaultProps} onVote={onVote} />);

            const button = screen.getByRole('button', { name: '투표하기' });
            fireEvent.click(button);

            // 비활성화 버튼은 클릭이 무시됨
            expect(onVote).not.toHaveBeenCalled();
        });
    });
});
