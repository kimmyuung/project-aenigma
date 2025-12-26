package com.aenigma.api.vote.dto;

import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.entity.VoteType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투표 응답 DTO
 */
@Getter
@Builder
public class VoteResponse {

    private UUID id;
    private UUID gameId;
    private PlayerInfo voter;
    private PlayerInfo target;
    private VoteType voteType;
    private int round;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class PlayerInfo {
        private UUID playerId;
        private UUID userId;
        private String nickname;
        private boolean isAlive;
    }

    public static VoteResponse from(Vote vote) {
        return VoteResponse.builder()
                .id(vote.getId())
                .gameId(vote.getGame().getId())
                .voter(PlayerInfo.builder()
                        .playerId(vote.getVoter().getId())
                        .userId(vote.getVoter().getUser().getId())
                        .nickname(vote.getVoter().getUser().getNickname())
                        .isAlive(vote.getVoter().getIsAlive())
                        .build())
                .target(PlayerInfo.builder()
                        .playerId(vote.getTarget().getId())
                        .userId(vote.getTarget().getUser().getId())
                        .nickname(vote.getTarget().getUser().getNickname())
                        .isAlive(vote.getTarget().getIsAlive())
                        .build())
                .voteType(vote.getVoteType())
                .round(vote.getRound())
                .createdAt(vote.getCreatedAt())
                .build();
    }
}
