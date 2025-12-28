package com.aenigma.api.game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 투표 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
public class VoteRequest {

    @NotNull(message = "투표 대상은 필수입니다.")
    private UUID targetPlayerId;

    public VoteRequest(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}
