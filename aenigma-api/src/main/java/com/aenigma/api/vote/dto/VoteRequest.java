package com.aenigma.api.vote.dto;

import com.aenigma.domain.vote.entity.VoteType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 투표 요청 DTO
 */
@Getter
@NoArgsConstructor
public class VoteRequest {

    @NotNull(message = "투표 대상을 선택해주세요.")
    private UUID targetId;

    @NotNull(message = "투표 유형을 선택해주세요.")
    private VoteType voteType;

    private int round;
}
