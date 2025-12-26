package com.aenigma.api.vote.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * 투표 결과 응답 DTO
 */
@Getter
@Builder
public class VoteResultResponse {

    private UUID gameId;
    private int round;
    private Map<UUID, Long> results; // playerId -> voteCount
    private boolean isComplete;
    private int totalVotes;
    private int expectedVotes;
}
