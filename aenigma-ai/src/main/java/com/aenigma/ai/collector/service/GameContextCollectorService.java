package com.aenigma.ai.collector.service;

import com.aenigma.ai.collector.entity.GameClue;
import com.aenigma.ai.collector.entity.GameSession;
import com.aenigma.ai.collector.repository.GameClueRepository;
import com.aenigma.ai.collector.repository.GameSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 컨텍스트 수집 서비스
 * 
 * 시나리오, 역할 배정, 단서 정보 등 AI 학습에 필요한 컨텍스트를 수집합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameContextCollectorService {

    private final GameClueRepository clueRepository;
    private final GameSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    // === 게임 세션 관리 ===

    /**
     * 게임 세션 시작 기록
     */
    public GameSession startSession(UUID gameId, String scenarioId, String scenarioTitle,
            String scenarioDescription, String scenarioDifficulty,
            int playerCount, int maxInvestigationRounds) {
        GameSession session = GameSession.builder()
                .gameId(gameId)
                .scenarioId(scenarioId)
                .scenarioTitle(scenarioTitle)
                .scenarioDescription(scenarioDescription)
                .scenarioDifficulty(scenarioDifficulty)
                .playerCount(playerCount)
                .maxInvestigationRounds(maxInvestigationRounds)
                .startedAt(LocalDateTime.now())
                .build();

        GameSession saved = sessionRepository.save(session);
        log.info("게임 세션 시작: gameId={}, scenario={}", gameId, scenarioTitle);
        return saved;
    }

    /**
     * 역할 배정 기록
     */
    public void recordRoleAssignments(UUID gameId, Map<UUID, String> roleAssignments) {
        sessionRepository.findByGameId(gameId).ifPresent(session -> {
            try {
                String json = objectMapper.writeValueAsString(roleAssignments);
                session.updateRoleAssignments(json);
                log.debug("역할 배정 기록: gameId={}, roles={}", gameId, roleAssignments.size());
            } catch (JsonProcessingException e) {
                log.error("역할 배정 JSON 변환 실패", e);
            }
        });
    }

    /**
     * 게임 종료 기록
     */
    public void finishSession(UUID gameId, String winningTeam, Map<String, Object> finalVotes) {
        sessionRepository.findByGameId(gameId).ifPresent(session -> {
            try {
                String votesJson = objectMapper.writeValueAsString(finalVotes);
                session.finishGame(winningTeam, votesJson);
                log.info("게임 세션 종료: gameId={}, winner={}", gameId, winningTeam);
            } catch (JsonProcessingException e) {
                log.error("최종 투표 JSON 변환 실패", e);
            }
        });
    }

    // === 단서 관리 ===

    /**
     * 개별 단서 기록 (플레이어 전용)
     */
    public void recordPrivateClue(UUID gameId, String scenarioId, String scenarioTitle,
            UUID playerId, String playerRole,
            String clueTitle, String clueContent) {
        GameClue clue = GameClue.createPrivateClue(
                gameId, scenarioId, scenarioTitle, playerId, playerRole, clueTitle, clueContent);
        clueRepository.save(clue);
        log.debug("개별 단서 기록: gameId={}, playerId={}, title={}", gameId, playerId, clueTitle);
    }

    /**
     * 공개 단서 기록
     */
    public void recordPublicClue(UUID gameId, String scenarioId, String scenarioTitle,
            String clueTitle, String clueContent,
            String revealedPhase, Integer revealedRound) {
        GameClue clue = GameClue.createPublicClue(
                gameId, scenarioId, scenarioTitle, clueTitle, clueContent, revealedPhase, revealedRound);
        clueRepository.save(clue);
        log.debug("공개 단서 기록: gameId={}, title={}, phase={}", gameId, clueTitle, revealedPhase);

        // 세션에 공개 단서 요약 업데이트
        updatePublicCluesSummary(gameId);
    }

    /**
     * 역할 기반 단서 기록
     */
    public void recordRoleClue(UUID gameId, String scenarioId, String scenarioTitle,
            UUID playerId, String playerRole,
            String clueTitle, String clueContent) {
        GameClue clue = GameClue.createRoleClue(
                gameId, scenarioId, scenarioTitle, playerId, playerRole, clueTitle, clueContent);
        clueRepository.save(clue);
        log.debug("역할 단서 기록: gameId={}, role={}, title={}", gameId, playerRole, clueTitle);
    }

    /**
     * 세션의 공개 단서 요약 업데이트
     */
    private void updatePublicCluesSummary(UUID gameId) {
        sessionRepository.findByGameId(gameId).ifPresent(session -> {
            List<GameClue> publicClues = clueRepository.findByGameIdAndIsPublicTrueOrderByCreatedAtAsc(gameId);
            try {
                List<Map<String, String>> summary = publicClues.stream()
                        .map(c -> Map.of("title", c.getClueTitle(), "content", c.getClueContent()))
                        .toList();
                session.updatePublicClues(objectMapper.writeValueAsString(summary));
            } catch (JsonProcessingException e) {
                log.error("공개 단서 요약 변환 실패", e);
            }
        });
    }

    // === 조회 ===

    @Transactional(readOnly = true)
    public GameSession getSession(UUID gameId) {
        return sessionRepository.findByGameId(gameId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GameClue> getPlayerClues(UUID gameId, UUID playerId) {
        return clueRepository.findByGameIdAndPlayerIdOrderByCreatedAtAsc(gameId, playerId);
    }

    @Transactional(readOnly = true)
    public List<GameClue> getPublicClues(UUID gameId) {
        return clueRepository.findByGameIdAndIsPublicTrueOrderByCreatedAtAsc(gameId);
    }

    @Transactional(readOnly = true)
    public List<GameClue> getAllClues(UUID gameId) {
        return clueRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    @Transactional(readOnly = true)
    public List<GameSession> getCompletedSessions() {
        return sessionRepository.findCompletedSessions();
    }
}
