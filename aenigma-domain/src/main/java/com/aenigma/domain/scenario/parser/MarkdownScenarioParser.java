package com.aenigma.domain.scenario.parser;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.ClueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 형식의 시나리오 파서
 * 
 * Markdown 형식으로 작성된 시나리오 파일을 파싱합니다.
 */
@Component
@Slf4j
public class MarkdownScenarioParser implements ScenarioParser {

    // 정규식 패턴들
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#\\s*(?:🎭\\s*)?시나리오[:\\s]*(.+)$",
            Pattern.MULTILINE);
    private static final Pattern PLAYERS_PATTERN = Pattern.compile("-\\s*플레이어[:\\s]*\\[(\\d+)~(\\d+)\\]\\s*명",
            Pattern.MULTILINE);
    private static final Pattern TIME_PATTERN = Pattern.compile("-\\s*예상\\s*시간[:\\s]*\\[(\\d+)\\]\\s*분",
            Pattern.MULTILINE);
    private static final Pattern DIFFICULTY_PATTERN = Pattern.compile("-\\s*난이도[:\\s]*\\[(\\d+)\\]", Pattern.MULTILINE);
    private static final Pattern TAGS_PATTERN = Pattern.compile("-\\s*태그[:\\s]*(.+)$", Pattern.MULTILINE);

    private static final Pattern ROLE_HEADER_PATTERN = Pattern.compile("^###\\s*\\[([A-Z_]+)\\]\\s*(.+)$",
            Pattern.MULTILINE);
    private static final Pattern CLUE_HEADER_PATTERN = Pattern
            .compile("^###\\s*\\[([A-Z_]+)(?::([^\\]]+))?\\]\\s*(.+)$", Pattern.MULTILINE);

    private static final Pattern FIELD_PATTERN = Pattern.compile("^-\\s*\\*\\*([^*]+)\\*\\*[:\\s]*(.*)$",
            Pattern.MULTILINE);

    @Override
    public ParsedScenario parse(String content) throws ScenarioParseException {
        if (content == null || content.isBlank()) {
            throw new ScenarioParseException("파일 내용이 비어있습니다.");
        }

        try {
            ParsedScenario scenario = ParsedScenario.builder()
                    .title(parseTitle(content))
                    .minPlayers(4) // 기본값
                    .maxPlayers(6) // 기본값
                    .estimatedMinutes(120)
                    .difficulty(3)
                    .build();

            // 기본 정보 파싱
            parseBasicInfo(content, scenario);

            // 배경 스토리 파싱
            scenario.setBackgroundStory(parseBackgroundStory(content));

            // 역할 파싱
            scenario.setRoles(parseRoles(content));

            // 단서 파싱
            scenario.setClues(parseClues(content));

            log.info("시나리오 파싱 완료: title={}, roles={}, clues={}",
                    scenario.getTitle(), scenario.getRoles().size(), scenario.getClues().size());

            return scenario;

        } catch (Exception e) {
            throw new ScenarioParseException("시나리오 파싱 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "md";
    }

    /**
     * 제목 파싱
     */
    private String parseTitle(String content) {
        Matcher matcher = TITLE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new ScenarioParseException("시나리오 제목을 찾을 수 없습니다. '# 시나리오: 제목' 형식으로 작성해주세요.");
    }

    /**
     * 기본 정보 파싱 (플레이어 수, 시간, 난이도, 태그)
     */
    private void parseBasicInfo(String content, ParsedScenario scenario) {
        // 플레이어 수
        Matcher playersMatcher = PLAYERS_PATTERN.matcher(content);
        if (playersMatcher.find()) {
            scenario.setMinPlayers(Integer.parseInt(playersMatcher.group(1)));
            scenario.setMaxPlayers(Integer.parseInt(playersMatcher.group(2)));
        }

        // 예상 시간
        Matcher timeMatcher = TIME_PATTERN.matcher(content);
        if (timeMatcher.find()) {
            scenario.setEstimatedMinutes(Integer.parseInt(timeMatcher.group(1)));
        }

        // 난이도
        Matcher difficultyMatcher = DIFFICULTY_PATTERN.matcher(content);
        if (difficultyMatcher.find()) {
            scenario.setDifficulty(Integer.parseInt(difficultyMatcher.group(1)));
        }

        // 태그
        Matcher tagsMatcher = TAGS_PATTERN.matcher(content);
        if (tagsMatcher.find()) {
            String tagsStr = tagsMatcher.group(1).trim();
            List<String> tags = Arrays.stream(tagsStr.split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            scenario.setTags(new ArrayList<>(tags));
        }
    }

    /**
     * 배경 스토리 파싱
     */
    private String parseBackgroundStory(String content) {
        // "## 📜 배경 스토리" 또는 "## 배경 스토리" 섹션 찾기
        Pattern storyPattern = Pattern.compile(
                "##\\s*(?:📜\\s*)?배경\\s*스토리\\s*\\n([\\s\\S]*?)(?=\\n---\\n|\\n##\\s|$)",
                Pattern.MULTILINE);
        Matcher matcher = storyPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 역할 목록 파싱
     */
    private List<ParsedScenario.ParsedRole> parseRoles(String content) {
        List<ParsedScenario.ParsedRole> roles = new ArrayList<>();

        // "## 🎭 역할" 또는 "## 역할" 섹션 찾기
        Pattern sectionPattern = Pattern.compile(
                "##\\s*(?:🎭\\s*)?역할\\s*\\n([\\s\\S]*?)(?=\\n---\\n|\\n##\\s(?!#)|$)",
                Pattern.MULTILINE);
        Matcher sectionMatcher = sectionPattern.matcher(content);

        if (!sectionMatcher.find()) {
            return roles;
        }

        String rolesSection = sectionMatcher.group(1);

        // 각 역할 블록 파싱
        String[] roleBlocks = rolesSection.split("(?=###\\s*\\[)");
        int order = 0;

        for (String block : roleBlocks) {
            if (block.isBlank())
                continue;

            Matcher headerMatcher = ROLE_HEADER_PATTERN.matcher(block);
            if (headerMatcher.find()) {
                String roleTypeStr = headerMatcher.group(1);
                String roleName = headerMatcher.group(2).trim();

                GameRole roleType = parseGameRole(roleTypeStr);

                ParsedScenario.ParsedRole role = ParsedScenario.ParsedRole.builder()
                        .name(roleName)
                        .roleType(roleType)
                        .displayOrder(order++)
                        .build();

                // 필드 파싱
                parseRoleFields(block, role);

                roles.add(role);
            }
        }

        return roles;
    }

    /**
     * 역할 필드 파싱
     */
    private void parseRoleFields(String block, ParsedScenario.ParsedRole role) {
        Matcher fieldMatcher = FIELD_PATTERN.matcher(block);
        StringBuilder currentField = null;
        String currentFieldName = null;

        String[] lines = block.split("\n");
        for (String line : lines) {
            Matcher matcher = FIELD_PATTERN.matcher(line);
            if (matcher.matches()) {
                // 이전 필드 저장
                if (currentFieldName != null && currentField != null) {
                    setRoleField(role, currentFieldName, currentField.toString().trim());
                }

                currentFieldName = matcher.group(1).trim();
                currentField = new StringBuilder(matcher.group(2).trim());
            } else if (currentField != null && !line.trim().startsWith("-") && !line.trim().startsWith("#")) {
                // 멀티라인 값 계속
                if (!line.isBlank()) {
                    currentField.append("\n").append(line.trim());
                }
            }
        }

        // 마지막 필드 저장
        if (currentFieldName != null && currentField != null) {
            setRoleField(role, currentFieldName, currentField.toString().trim());
        }
    }

    /**
     * 역할 필드 설정
     */
    private void setRoleField(ParsedScenario.ParsedRole role, String fieldName, String value) {
        switch (fieldName) {
            case "설명" -> role.setDescription(value);
            case "비밀" -> role.setSecretInfo(value);
            case "목표" -> role.setObjective(value);
            case "관계" -> role.setRelationships(value);
        }
    }

    /**
     * 단서 목록 파싱
     */
    private List<ParsedScenario.ParsedClue> parseClues(String content) {
        List<ParsedScenario.ParsedClue> clues = new ArrayList<>();

        // "## 🔍 단서" 또는 "## 단서" 섹션 찾기
        Pattern sectionPattern = Pattern.compile(
                "##\\s*(?:🔍\\s*)?단서\\s*\\n([\\s\\S]*?)(?=\\n---\\n|\\n##\\s(?!#)|$)",
                Pattern.MULTILINE);
        Matcher sectionMatcher = sectionPattern.matcher(content);

        if (!sectionMatcher.find()) {
            return clues;
        }

        String cluesSection = sectionMatcher.group(1);

        // 각 단서 블록 파싱
        String[] clueBlocks = cluesSection.split("(?=###\\s*\\[)");
        int order = 0;

        for (String block : clueBlocks) {
            if (block.isBlank())
                continue;

            Matcher headerMatcher = CLUE_HEADER_PATTERN.matcher(block);
            if (headerMatcher.find()) {
                String clueTypeStr = headerMatcher.group(1);
                String assignedRoleName = headerMatcher.group(2); // PERSONAL:역할명 인 경우
                String clueTitle = headerMatcher.group(3).trim();

                ClueType clueType = parseClueType(clueTypeStr);

                ParsedScenario.ParsedClue clue = ParsedScenario.ParsedClue.builder()
                        .title(clueTitle)
                        .clueType(clueType)
                        .assignedRoleName(assignedRoleName != null ? assignedRoleName.trim() : null)
                        .importance(3) // 기본값
                        .displayOrder(order++)
                        .build();

                // 필드 파싱
                parseClueFields(block, clue);

                clues.add(clue);
            }
        }

        return clues;
    }

    /**
     * 단서 필드 파싱
     */
    private void parseClueFields(String block, ParsedScenario.ParsedClue clue) {
        String[] lines = block.split("\n");
        String currentFieldName = null;
        StringBuilder currentField = null;

        for (String line : lines) {
            Matcher matcher = FIELD_PATTERN.matcher(line);
            if (matcher.matches()) {
                // 이전 필드 저장
                if (currentFieldName != null && currentField != null) {
                    setClueField(clue, currentFieldName, currentField.toString().trim());
                }

                currentFieldName = matcher.group(1).trim();
                currentField = new StringBuilder(matcher.group(2).trim());
            } else if (currentField != null && !line.trim().startsWith("-") && !line.trim().startsWith("#")) {
                // 멀티라인 값 계속
                if (!line.isBlank()) {
                    currentField.append("\n").append(line.trim());
                }
            }
        }

        // 마지막 필드 저장
        if (currentFieldName != null && currentField != null) {
            setClueField(clue, currentFieldName, currentField.toString().trim());
        }
    }

    /**
     * 단서 필드 설정
     */
    private void setClueField(ParsedScenario.ParsedClue clue, String fieldName, String value) {
        switch (fieldName) {
            case "내용" -> clue.setContent(value);
            case "라운드" -> {
                try {
                    clue.setRevealRound(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "중요도" -> {
                try {
                    clue.setImportance(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * GameRole 문자열을 enum으로 변환
     */
    private GameRole parseGameRole(String roleTypeStr) {
        return switch (roleTypeStr.toUpperCase()) {
            case "CRIMINAL", "MURDERER", "범인" -> GameRole.CRIMINAL;
            case "DETECTIVE", "탐정" -> GameRole.DETECTIVE;
            default -> GameRole.SUSPECT;
        };
    }

    /**
     * ClueType 문자열을 enum으로 변환
     */
    private ClueType parseClueType(String clueTypeStr) {
        return switch (clueTypeStr.toUpperCase()) {
            case "PUBLIC", "공개" -> ClueType.PUBLIC;
            case "PERSONAL", "개인" -> ClueType.PERSONAL;
            case "HIDDEN", "숨김" -> ClueType.HIDDEN;
            default -> ClueType.PUBLIC;
        };
    }
}
