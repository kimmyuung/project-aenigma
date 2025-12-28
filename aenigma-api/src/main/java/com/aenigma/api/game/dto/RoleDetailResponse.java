package com.aenigma.api.game.dto;

import com.aenigma.domain.game.entity.GamePlayer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 역할 상세 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailResponse {

    private UUID playerId;
    private String nickname;
    private String displayTag;

    // 기본 역할 정보
    private String roleType; // CRIMINAL, DETECTIVE, SUSPECT

    // 시나리오 역할 정보 (시나리오 기반 게임인 경우)
    private String roleName;
    private String description;
    private String secretInfo;
    private String objective;
    private String alibi; // JSON 형식: [{"time":"09:00","location":"식당","activity":"아침 식사"}]

    private boolean isAlive;

    public static RoleDetailResponse from(GamePlayer player) {
        return RoleDetailResponse.builder()
                .playerId(player.getId())
                .nickname(player.getUser().getNickname())
                .displayTag(player.getUser().getDisplayTag())
                .roleType(player.getRole().name())
                .roleName(player.getScenarioRoleName())
                .description(player.getScenarioRoleDescription())
                .secretInfo(player.getScenarioRoleSecret())
                .objective(player.getScenarioRoleObjective())
                .alibi(player.getScenarioRoleAlibi())
                .isAlive(player.getIsAlive())
                .build();
    }
}
