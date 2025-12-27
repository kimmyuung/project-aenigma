package com.aenigma.domain.scenario.service;

import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.scenario.entity.*;
import com.aenigma.domain.scenario.parser.MarkdownScenarioParser;
import com.aenigma.domain.scenario.parser.ParsedScenario;
import com.aenigma.domain.scenario.parser.ScenarioParseException;
import com.aenigma.domain.scenario.repository.ScenarioRepository;
import com.aenigma.domain.user.entity.User;
import com.aenigma.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 시나리오 업로드 서비스
 * 
 * 파일을 파싱하여 시나리오(역할, 단서 포함)를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScenarioUploadService {

    private final ScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final MarkdownScenarioParser markdownParser;

    /**
     * Markdown 파일 내용으로 시나리오 생성
     * 
     * @param authorId        작가 ID
     * @param markdownContent Markdown 파일 내용
     * @param price           가격 (null이면 무료)
     * @return 생성된 시나리오
     */
    public Scenario uploadFromMarkdown(UUID authorId, String markdownContent, BigDecimal price) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + authorId));

        // 파싱
        ParsedScenario parsed = markdownParser.parse(markdownContent);

        // 시나리오 엔티티 생성
        Scenario scenario = createScenarioFromParsed(author, parsed, price);

        // 역할 생성 및 매핑 (단서 연결용)
        Map<String, ScenarioRole> roleMap = new HashMap<>();
        for (ParsedScenario.ParsedRole parsedRole : parsed.getRoles()) {
            ScenarioRole role = createRoleFromParsed(parsedRole);
            scenario.addRole(role);
            roleMap.put(role.getName(), role);
        }

        // 단서 생성
        for (ParsedScenario.ParsedClue parsedClue : parsed.getClues()) {
            ScenarioClue clue = createClueFromParsed(parsedClue, roleMap);
            scenario.addClue(clue);
        }

        // 탐정 역할 포함 여부 설정
        boolean hasDetective = scenario.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == GameRole.DETECTIVE);
        scenario = Scenario.builder()
                .id(scenario.getId())
                .title(scenario.getTitle())
                .description(scenario.getDescription())
                .backgroundStory(scenario.getBackgroundStory())
                .author(scenario.getAuthor())
                .minPlayers(scenario.getMinPlayers())
                .maxPlayers(scenario.getMaxPlayers())
                .estimatedMinutes(scenario.getEstimatedMinutes())
                .difficulty(scenario.getDifficulty())
                .tags(scenario.getTags())
                .price(scenario.getPrice())
                .status(ScenarioStatus.DRAFT)
                .hasDetective(hasDetective)
                .roles(scenario.getRoles())
                .clues(scenario.getClues())
                .build();

        // 저장
        scenarioRepository.save(scenario);

        log.info("시나리오 업로드 완료: id={}, title={}, roles={}, clues={}",
                scenario.getId(), scenario.getTitle(),
                scenario.getRoles().size(), scenario.getClues().size());

        return scenario;
    }

    /**
     * ParsedScenario에서 Scenario 엔티티 생성
     */
    private Scenario createScenarioFromParsed(User author, ParsedScenario parsed, BigDecimal price) {
        return Scenario.builder()
                .author(author)
                .title(parsed.getTitle())
                .description(parsed.getDescription())
                .backgroundStory(parsed.getBackgroundStory())
                .minPlayers(parsed.getMinPlayers())
                .maxPlayers(parsed.getMaxPlayers())
                .estimatedMinutes(parsed.getEstimatedMinutes())
                .difficulty(parsed.getDifficulty())
                .tags(parsed.getTags())
                .price(price != null ? price : BigDecimal.ZERO)
                .status(ScenarioStatus.DRAFT)
                .build();
    }

    /**
     * ParsedRole에서 ScenarioRole 엔티티 생성
     */
    private ScenarioRole createRoleFromParsed(ParsedScenario.ParsedRole parsed) {
        return ScenarioRole.builder()
                .name(parsed.getName())
                .roleType(parsed.getRoleType())
                .description(parsed.getDescription())
                .secretInfo(parsed.getSecretInfo())
                .objective(parsed.getObjective())
                .relationships(parsed.getRelationships())
                .displayOrder(parsed.getDisplayOrder())
                .build();
    }

    /**
     * ParsedClue에서 ScenarioClue 엔티티 생성
     */
    private ScenarioClue createClueFromParsed(ParsedScenario.ParsedClue parsed,
            Map<String, ScenarioRole> roleMap) {
        ScenarioRole assignedRole = null;

        // PERSONAL 타입인 경우 역할 연결
        if (parsed.getClueType() == ClueType.PERSONAL && parsed.getAssignedRoleName() != null) {
            assignedRole = roleMap.get(parsed.getAssignedRoleName());
            if (assignedRole == null) {
                log.warn("단서 '{}'에 지정된 역할 '{}'을 찾을 수 없습니다.",
                        parsed.getTitle(), parsed.getAssignedRoleName());
            }
        }

        return ScenarioClue.builder()
                .title(parsed.getTitle())
                .content(parsed.getContent())
                .clueType(parsed.getClueType())
                .assignedRole(assignedRole)
                .revealRound(parsed.getRevealRound())
                .importance(parsed.getImportance())
                .displayOrder(parsed.getDisplayOrder())
                .build();
    }

    /**
     * 업로드 유효성 검증
     */
    public void validateMarkdown(String markdownContent) throws ScenarioParseException {
        ParsedScenario parsed = markdownParser.parse(markdownContent);

        if (parsed.getTitle() == null || parsed.getTitle().isBlank()) {
            throw new ScenarioParseException("시나리오 제목이 필요합니다.");
        }

        if (parsed.getRoles().isEmpty()) {
            throw new ScenarioParseException("최소 1개 이상의 역할이 필요합니다.");
        }

        // 범인 역할 확인
        boolean hasCriminal = parsed.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == GameRole.CRIMINAL);
        if (!hasCriminal) {
            throw new ScenarioParseException("최소 1명의 범인(CRIMINAL) 역할이 필요합니다.");
        }
    }
}
