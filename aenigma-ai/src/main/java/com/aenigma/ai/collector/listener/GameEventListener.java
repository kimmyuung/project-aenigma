package com.aenigma.ai.collector.listener;

import com.aenigma.ai.collector.service.GameEventCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 게임 이벤트 리스너
 * 
 * 게임에서 발생하는 이벤트를 수신하여 DB에 저장합니다.
 * Socket 모듈에서 ApplicationEvent를 발행하면 이 리스너가 수신합니다.
 * 
 * 사용 예시 (Socket 모듈에서):
 * applicationEventPublisher.publishEvent(new GameChatEvent(gameId, playerId,
 * content, ...));
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventListener {

    private final GameEventCollectorService collectorService;

    /**
     * 채팅 이벤트 수신
     */
    @EventListener
    public void handleChatEvent(GameChatEvent event) {
        log.debug("채팅 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordChatMessage(
                event.gameId(),
                event.playerId(),
                event.playerRole(),
                event.content(),
                event.phase());
    }

    /**
     * 귓속말 이벤트 수신
     */
    @EventListener
    public void handleWhisperEvent(GameWhisperEvent event) {
        log.debug("귓속말 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordWhisper(
                event.gameId(),
                event.senderId(),
                event.receiverId(),
                event.senderRole(),
                event.content(),
                event.phase());
    }

    /**
     * 투표 이벤트 수신
     */
    @EventListener
    public void handleVoteEvent(GameVoteEvent event) {
        log.debug("투표 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordVote(
                event.gameId(),
                event.voterId(),
                event.targetId(),
                event.voterRole(),
                event.round());
    }

    /**
     * 페이즈 전환 이벤트 수신
     */
    @EventListener
    public void handlePhaseChangeEvent(GamePhaseChangeEvent event) {
        log.debug("페이즈 변경 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordPhaseChange(
                event.gameId(),
                event.fromPhase(),
                event.toPhase(),
                event.round());
    }

    /**
     * 게임 시작 이벤트 수신
     */
    @EventListener
    public void handleGameStartEvent(GameStartEvent event) {
        log.info("게임 시작 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordGameStart(event.gameId(), event.playerCount());
    }

    /**
     * 게임 종료 이벤트 수신
     */
    @EventListener
    public void handleGameEndEvent(GameEndEvent event) {
        log.info("게임 종료 이벤트 수신: gameId={}", event.gameId());
        collectorService.recordGameEnd(event.gameId(), event.winningTeam(), event.metadata());
    }

    // === 이벤트 레코드 ===

    public record GameChatEvent(
            java.util.UUID gameId,
            java.util.UUID playerId,
            String playerRole,
            String content,
            String phase) {
    }

    public record GameWhisperEvent(
            java.util.UUID gameId,
            java.util.UUID senderId,
            java.util.UUID receiverId,
            String senderRole,
            String content,
            String phase) {
    }

    public record GameVoteEvent(
            java.util.UUID gameId,
            java.util.UUID voterId,
            java.util.UUID targetId,
            String voterRole,
            int round) {
    }

    public record GamePhaseChangeEvent(
            java.util.UUID gameId,
            String fromPhase,
            String toPhase,
            int round) {
    }

    public record GameStartEvent(
            java.util.UUID gameId,
            int playerCount) {
    }

    public record GameEndEvent(
            java.util.UUID gameId,
            String winningTeam,
            String metadata) {
    }
}
