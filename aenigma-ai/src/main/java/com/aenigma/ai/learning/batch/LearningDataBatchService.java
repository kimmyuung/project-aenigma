package com.aenigma.ai.learning.batch;

import com.aenigma.ai.collector.entity.GameClue;
import com.aenigma.ai.collector.entity.GameEvent;
import com.aenigma.ai.collector.entity.GameSession;
import com.aenigma.ai.collector.repository.GameClueRepository;
import com.aenigma.ai.collector.repository.GameEventRepository;
import com.aenigma.ai.collector.repository.GameSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 학습 데이터 배치 서비스
 * 
 * 완료된 게임 데이터를 배치로 처리하여 AI 학습용 데이터셋을 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningDataBatchService {

    private final GameSessionRepository sessionRepository;
    private final GameEventRepository eventRepository;
    private final GameClueRepository clueRepository;
    private final ObjectMapper objectMapper;

    /**
     * 매일 새벽 3시에 배치 실행 (운영 환경용)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledBatchProcess() {
        log.info("=== AI 학습 데이터 배치 시작 ===");
        try {
            BatchResult result = processBatch();
            log.info("=== AI 학습 데이터 배치 완료: {} 게임 처리 ===", result.getProcessedGames());
        } catch (Exception e) {
            log.error("AI 학습 데이터 배치 실패", e);
        }
    }

    /**
     * 수동 배치 실행
     */
    @Transactional(readOnly = true)
    public BatchResult processBatch() {
        List<GameSession> completedSessions = sessionRepository.findCompletedSessions();

        if (completedSessions.isEmpty()) {
            log.info("처리할 완료된 게임이 없습니다.");
            return BatchResult.empty();
        }

        List<TrainingDataset> datasets = new ArrayList<>();

        for (GameSession session : completedSessions) {
            try {
                TrainingDataset dataset = buildTrainingDataset(session);
                datasets.add(dataset);
            } catch (Exception e) {
                log.error("게임 데이터 처리 실패: gameId={}", session.getGameId(), e);
            }
        }

        log.info("배치 처리 완료: {} 게임, {} 이벤트",
                datasets.size(),
                datasets.stream().mapToInt(d -> d.getEvents().size()).sum());

        return BatchResult.builder()
                .processedGames(datasets.size())
                .totalEvents(datasets.stream().mapToInt(d -> d.getEvents().size()).sum())
                .datasets(datasets)
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 특정 게임의 학습 데이터셋 생성
     */
    @Transactional(readOnly = true)
    public TrainingDataset buildTrainingDataset(GameSession session) {
        UUID gameId = session.getGameId();

        // 학습용 이벤트 조회 (INTRO, LOBBY 제외)
        List<GameEvent> events = eventRepository.findLearnableEventsByGameId(gameId);

        // 단서 조회
        List<GameClue> allClues = clueRepository.findByGameIdOrderByCreatedAtAsc(gameId);
        List<GameClue> publicClues = allClues.stream()
                .filter(GameClue::isPublic)
                .toList();

        // 역할별 개별 단서 매핑
        Map<String, List<GameClue>> cluesByRole = allClues.stream()
                .filter(c -> !c.isPublic() && c.getPlayerRole() != null)
                .collect(Collectors.groupingBy(GameClue::getPlayerRole));

        // 이벤트를 학습 샘플로 변환
        List<TrainingSample> samples = events.stream()
                .filter(e -> e.getContent() != null && !e.getContent().isBlank())
                .map(event -> buildTrainingSample(event, session, publicClues, cluesByRole))
                .toList();

        return TrainingDataset.builder()
                .gameId(gameId)
                .scenarioId(session.getScenarioId())
                .scenarioTitle(session.getScenarioTitle())
                .playerCount(session.getPlayerCount())
                .winningTeam(session.getWinningTeam())
                .gameDurationMinutes(session.getGameDurationMinutes())
                .events(samples)
                .publicClueCount(publicClues.size())
                .totalEventCount(events.size())
                .build();
    }

    /**
     * 개별 이벤트를 학습 샘플로 변환
     */
    private TrainingSample buildTrainingSample(GameEvent event, GameSession session,
            List<GameClue> publicClues,
            Map<String, List<GameClue>> cluesByRole) {
        // 해당 플레이어의 개별 단서
        List<String> playerClues = new ArrayList<>();
        if (event.getPlayerRole() != null && cluesByRole.containsKey(event.getPlayerRole())) {
            playerClues = cluesByRole.get(event.getPlayerRole()).stream()
                    .map(c -> c.getClueTitle() + ": " + c.getClueContent())
                    .toList();
        }

        // 공개 단서 (이벤트 시점까지)
        List<String> publicClueTexts = publicClues.stream()
                .filter(c -> c.getCreatedAt().isBefore(event.getCreatedAt()) ||
                        c.getCreatedAt().equals(event.getCreatedAt()))
                .map(c -> c.getClueTitle() + ": " + c.getClueContent())
                .toList();

        return TrainingSample.builder()
                .eventType(event.getEventType().name())
                .playerRole(event.getPlayerRole())
                .phase(event.getPhase())
                .round(event.getRound())
                .content(event.getContent())
                .targetPlayerId(event.getTargetPlayerId() != null ? event.getTargetPlayerId().toString() : null)
                .playerKnownClues(playerClues)
                .publicCluesAtTime(publicClueTexts)
                .scenarioId(session.getScenarioId())
                .winningTeam(session.getWinningTeam())
                .isWinner(isPlayerOnWinningTeam(event.getPlayerRole(), session.getWinningTeam()))
                .timestamp(event.getCreatedAt())
                .build();
    }

    /**
     * 플레이어가 승리팀인지 확인
     */
    private boolean isPlayerOnWinningTeam(String playerRole, String winningTeam) {
        if (playerRole == null || winningTeam == null)
            return false;

        // CRIMINAL이 승리하면 범인팀 승리, 아니면 시민팀 승리
        if ("CRIMINAL".equalsIgnoreCase(winningTeam)) {
            return "CRIMINAL".equalsIgnoreCase(playerRole);
        } else {
            return !"CRIMINAL".equalsIgnoreCase(playerRole);
        }
    }

    // === DTO 클래스 ===

    @Getter
    @Builder
    public static class BatchResult {
        private int processedGames;
        private int totalEvents;
        private List<TrainingDataset> datasets;
        private LocalDateTime processedAt;

        public static BatchResult empty() {
            return BatchResult.builder()
                    .processedGames(0)
                    .totalEvents(0)
                    .datasets(Collections.emptyList())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TrainingDataset {
        private UUID gameId;
        private String scenarioId;
        private String scenarioTitle;
        private int playerCount;
        private String winningTeam;
        private Integer gameDurationMinutes;
        private List<TrainingSample> events;
        private int publicClueCount;
        private int totalEventCount;
    }

    @Getter
    @Builder
    public static class TrainingSample {
        private String eventType;
        private String playerRole;
        private String phase;
        private Integer round;
        private String content;
        private String targetPlayerId;
        private List<String> playerKnownClues;
        private List<String> publicCluesAtTime;
        private String scenarioId;
        private String winningTeam;
        private boolean isWinner;
        private LocalDateTime timestamp;
    }
}
