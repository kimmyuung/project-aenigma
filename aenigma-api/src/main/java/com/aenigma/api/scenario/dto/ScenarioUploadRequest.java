package com.aenigma.api.scenario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 시나리오 업로드 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioUploadRequest {

    /**
     * 가격 (null이면 무료)
     */
    private BigDecimal price;

    /**
     * 업로드 후 바로 공개할지 여부
     */
    @Builder.Default
    private boolean publishImmediately = false;
}
