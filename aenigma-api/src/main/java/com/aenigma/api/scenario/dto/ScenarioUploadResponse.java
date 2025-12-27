package com.aenigma.api.scenario.dto;

import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.entity.ScenarioClue;
import com.aenigma.domain.scenario.entity.ScenarioRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 시나리오 업로드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioUploadResponse {

    private UUID id;
    private String title;
    private String status;
    private int roleCount;
    private int clueCount;
    private BigDecimal price;
    private String message;

    /**
     * 역할 요약 목록
     */
    private List<RoleSummary> roles;

    /**
     * 단서 요약 목록
     */
    private List<ClueSummary> clues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleSummary {
        private UUID id;
        private String name;
        private String roleType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClueSummary {
        private UUID id;
        private String title;
        private String clueType;
    }

    public static ScenarioUploadResponse from(Scenario scenario) {
        return ScenarioUploadResponse.builder()
                .id(scenario.getId())
                .title(scenario.getTitle())
                .status(scenario.getStatus().name())
                .roleCount(scenario.getRoles().size())
                .clueCount(scenario.getClues().size())
                .price(scenario.getPrice())
                .message("시나리오가 성공적으로 업로드되었습니다.")
                .roles(scenario.getRoles().stream()
                        .map(role -> RoleSummary.builder()
                                .id(role.getId())
                                .name(role.getName())
                                .roleType(role.getRoleType().name())
                                .build())
                        .toList())
                .clues(scenario.getClues().stream()
                        .map(clue -> ClueSummary.builder()
                                .id(clue.getId())
                                .title(clue.getTitle())
                                .clueType(clue.getClueType().name())
                                .build())
                        .toList())
                .build();
    }
}
