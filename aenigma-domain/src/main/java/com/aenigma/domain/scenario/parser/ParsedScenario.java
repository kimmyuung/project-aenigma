package com.aenigma.domain.scenario.parser;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.ClueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 파싱된 시나리오 데이터
 * 
 * 파서가 파일을 파싱한 결과를 담는 DTO입니다.
 * 엔티티로 변환하기 전 중간 데이터 구조입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedScenario {

    // === 기본 정보 ===
    private String title;
    private String description;
    private String backgroundStory;
    private int minPlayers;
    private int maxPlayers;
    private int estimatedMinutes;
    private int difficulty;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // === 역할 목록 ===
    @Builder.Default
    private List<ParsedRole> roles = new ArrayList<>();

    // === 단서 목록 ===
    @Builder.Default
    private List<ParsedClue> clues = new ArrayList<>();

    /**
     * 파싱된 역할 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedRole {
        private String name;
        private GameRole roleType;
        private String description;
        private String secretInfo;
        private String objective;
        private String relationships;
        private int displayOrder;
    }

    /**
     * 파싱된 단서 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedClue {
        private String title;
        private String content;
        private ClueType clueType;
        private String assignedRoleName; // PERSONAL 타입일 때 역할 이름
        private Integer revealRound;
        private int importance;
        private int displayOrder;
    }
}
