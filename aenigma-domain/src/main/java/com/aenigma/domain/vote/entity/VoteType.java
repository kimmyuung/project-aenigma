package com.aenigma.domain.vote.entity;

/**
 * 투표 유형
 */
public enum VoteType {

    /**
     * 최종 범인 지목 투표
     */
    FINAL_VOTE,

    /**
     * 토론 중 일반 투표
     */
    DISCUSSION_VOTE,

    /**
     * 플레이어 추방 투표
     */
    ELIMINATION_VOTE
}
