package com.aenigma.ai.collector.service;

import com.aenigma.ai.collector.entity.GameEvent;
import com.aenigma.ai.collector.entity.GameEventType;
import com.aenigma.ai.collector.repository.GameEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 게임 이벤트 수집 서비스
 * 
 * 모든 게임 이벤트를 DB에 저장하여 AI 학습 데이터로 활용합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GameEventCollectorService {

    private final GameEventRepository eventRepository;

    /**
     * 공개 채팅 메시지 기록
     */
    public void recordChatMessage(UUID gameId, UUID playerId, String playerRole,
            String content, String phase) {
        GameEvent event = GameEvent.chatMessage(gameId, playerId, playerRole, content, phase, "PUBLIC");
        eventRepository.save(event);
        log.debug("채팅 이벤트 기록: gameId={}, playerId={}", gameId, playerId);
    }

    /**
     * 귓속말/밀담 기록
     */
    public void recordWhisper(UUID gameId, UUID senderId, UUID receiverId,
            String senderRole, String content, String phase) {
        GameEvent event = GameEvent.whisper(gameId, senderId, receiverId, senderRole, content, phase);
        eventRepository.save(event);
        log.debug("귓속말 이벤트 기록: gameId={}, sender={}, receiver={}", gameId, senderId, receiverId);
    }

    /**
     * 투표 기록
     */
    public void recordVote(UUID gameId, UUID voterId, UUID targetId,
            String voterRole, int round) {
        GameEvent event = GameEvent.vote(gameId, voterId, targetId, voterRole, round);
        eventRepository.save(event);
        log.debug("투표 이벤트 기록: gameId={}, voter={}, target={}", gameId, voterId, targetId);
    }

    /**
     * 페이즈 전환 기록
     */
    public void recordPhaseChange(UUID gameId, String fromPhase, String toPhase, int round) {
        GameEvent event = GameEvent.phaseChange(gameId, fromPhase, toPhase, round);
        eventRepository.save(event);
        log.debug("페이즈 전환 기록: gameId={}, {} -> {}", gameId, fromPhase, toPhase);
    }

    /**
     * 플레이어 탈락 기록
     */
    public void recordPlayerEliminated(UUID gameId, UUID playerId, String playerRole,
            String phase, String reason) {
        GameEvent event = GameEvent.playerEliminated(gameId, playerId, playerRole, phase, reason);
        eventRepository.save(event);
        log.debug("플레이어 탈락 기록: gameId={}, playerId={}", gameId, playerId);
    }

    /**
     * 게임 시작 기록
     */
    public void recordGameStart(UUID gameId, int playerCount) {
        GameEvent event = GameEvent.gameStart(gameId, playerCount);
        eventRepository.save(event);
        log.info("게임 시작 기록: gameId={}, playerCount={}", gameId, playerCount);
    }

    /**
     * 게임 종료 기록
     */
    public void recordGameEnd(UUID gameId, String winningTeam, String metadata) {
        GameEvent event = GameEvent.gameEnd(gameId, winningTeam, metadata);
        eventRepository.save(event);
        log.info("게임 종료 기록: gameId={}, winner={}", gameId, winningTeam);
    }

    /**
     * 역할 배정 기록
     */
    public void recordRoleAssignment(UUID gameId, UUID playerId, String role) {
        GameEvent event = GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.ROLE_ASSIGNED)
                .playerId(playerId)
                .playerRole(role)
                .build();
        eventRepository.save(event);
        log.debug("역할 배정 기록: gameId={}, playerId={}, role={}", gameId, playerId, role);
    }

    // === 조회 메서드 ===

    @Transactional(readOnly = true)
    public List<GameEvent> getGameEvents(UUID gameId) {
        return eventRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    @Transactional(readOnly = true)
    public List<GameEvent> getChatEvents(UUID gameId) {
        return eventRepository.findChatEventsByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public List<GameEvent> getVoteEvents(UUID gameId) {
        return eventRepository.findVoteEventsByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public List<GameEvent> getEventsByRole(String role) {
        return eventRepository.findEventsByRole(role);
    }

    @Transactional(readOnly = true)
    public long getCompletedGameCount() {
        return eventRepository.countCompletedGames();
    }
}
