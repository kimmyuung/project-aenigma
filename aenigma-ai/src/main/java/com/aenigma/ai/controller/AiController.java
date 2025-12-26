package com.aenigma.ai.controller;

import com.aenigma.ai.assistant.service.NarrationService;
import com.aenigma.ai.assistant.service.ScenarioRecommendService;
import com.aenigma.ai.collector.entity.GameEvent;
import com.aenigma.ai.collector.service.GameEventCollectorService;
import com.aenigma.ai.learning.batch.LearningDataBatchService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 모듈 REST API Controller
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final ScenarioRecommendService scenarioRecommendService;
    private final NarrationService narrationService;
    private final GameEventCollectorService eventCollectorService;
    private final LearningDataBatchService batchService;

    // === GM 보조 기능 ===

    /**
     * 시나리오 추천
     */
    @GetMapping("/scenarios/recommend")
    public ResponseEntity<ScenarioRecommendService.ScenarioRecommendation> recommendScenario(
            @RequestParam int playerCount) {
        log.info("시나리오 추천 요청: playerCount={}", playerCount);
        return ResponseEntity.ok(scenarioRecommendService.recommend(playerCount));
    }

    /**
     * 내레이션 생성
     */
    @PostMapping("/narration")
    public ResponseEntity<NarrationResponse> generateNarration(@RequestBody NarrationRequest request) {
        log.info("내레이션 생성 요청: type={}", request.getType());

        String narration = switch (request.getType()) {
            case "PHASE_TRANSITION" -> narrationService.generatePhaseTransition(
                    NarrationService.PhaseTransitionContext.builder()
                            .fromPhase(request.getFromPhase())
                            .toPhase(request.getToPhase())
                            .round(request.getRound())
                            .eliminatedCount(request.getEliminatedCount())
                            .build());
            case "GAME_START" -> narrationService.generateGameStartNarration(
                    request.getScenarioTitle(), request.getPlayerCount());
            case "ROLE_ASSIGNMENT" -> narrationService.generateRoleAssignmentNotice();
            default -> "알 수 없는 내레이션 유형입니다.";
        };

        return ResponseEntity.ok(new NarrationResponse(narration));
    }

    // === 이벤트 수집 API ===

    /**
     * 게임 이벤트 조회
     */
    @GetMapping("/events/{gameId}")
    public ResponseEntity<List<GameEvent>> getGameEvents(@PathVariable UUID gameId) {
        log.info("게임 이벤트 조회: gameId={}", gameId);
        return ResponseEntity.ok(eventCollectorService.getGameEvents(gameId));
    }

    /**
     * 채팅 이벤트만 조회 (AI 학습용 - INTRO, LOBBY 제외)
     */
    @GetMapping("/events/{gameId}/chats")
    public ResponseEntity<List<GameEvent>> getChatEvents(@PathVariable UUID gameId) {
        log.info("채팅 이벤트 조회: gameId={}", gameId);
        return ResponseEntity.ok(eventCollectorService.getChatEvents(gameId));
    }

    /**
     * 특정 역할의 행동 패턴 조회 (AI 학습용)
     */
    @GetMapping("/events/by-role/{role}")
    public ResponseEntity<List<GameEvent>> getEventsByRole(@PathVariable String role) {
        log.info("역할별 이벤트 조회: role={}", role);
        return ResponseEntity.ok(eventCollectorService.getEventsByRole(role));
    }

    /**
     * 학습 데이터 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long completedGames = eventCollectorService.getCompletedGameCount();
        return ResponseEntity.ok(Map.of(
                "completedGames", completedGames,
                "status", completedGames > 0 ? "학습 데이터 수집 중" : "데이터 없음"));
    }

    // === 배치 학습 API ===

    /**
     * 수동 배치 실행
     */
    @PostMapping("/learning/batch")
    public ResponseEntity<LearningDataBatchService.BatchResult> runBatch() {
        log.info("배치 학습 수동 실행 요청");
        LearningDataBatchService.BatchResult result = batchService.processBatch();
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 게임의 학습 데이터셋 조회
     */
    @GetMapping("/learning/dataset/{gameId}")
    public ResponseEntity<LearningDataBatchService.TrainingDataset> getDataset(@PathVariable UUID gameId) {
        log.info("학습 데이터셋 조회: gameId={}", gameId);
        // Session 조회 후 데이터셋 생성
        return ResponseEntity.ok(null); // TODO: implement
    }

    // === DTOs ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NarrationRequest {
        private String type; // PHASE_TRANSITION, GAME_START, ROLE_ASSIGNMENT
        private String fromPhase;
        private String toPhase;
        private int round;
        private int eliminatedCount;
        private String scenarioTitle;
        private int playerCount;
    }

    @Getter
    @AllArgsConstructor
    public static class NarrationResponse {
        private String narration;
    }
}
