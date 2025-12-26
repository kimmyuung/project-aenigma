package com.aenigma.ai.collector.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 이벤트 저장 엔티티
 * 
 * AI 학습을 위해 게임에서 발생하는 모든 이벤트를 기록합니다.
 * 채팅, 투표, 페이즈 전환 등 모든 행동이 저장됩니다.
 */
@Entity
@Table(name = "game_events", indexes = {
        @Index(name = "idx_event_game", columnList = "game_id"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private GameEventType eventType;

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "player_role", length = 50)
    private String playerRole;

    @Column(name = "target_player_id")
    private UUID targetPlayerId;

    // === 컨텍스트 정보 (AI 학습용) ===

    @Column(name = "scenario_id", length = 100)
    private String scenarioId;

    @Column(name = "player_known_clues", columnDefinition = "TEXT")
    private String playerKnownClues; // JSON: 이벤트 발생 시점의 플레이어 보유 단서

    @Column(name = "public_clues_at_time", columnDefinition = "TEXT")
    private String publicCluesAtTime; // JSON: 이벤트 발생 시점의 공개 단서

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "round")
    private Integer round;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON 형식 추가 데이터

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // === Factory Methods ===

    public static GameEvent chatMessage(UUID gameId, UUID playerId, String playerRole,
            String content, String phase, String messageType) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.CHAT_MESSAGE)
                .playerId(playerId)
                .playerRole(playerRole)
                .content(content)
                .phase(phase)
                .metadata("{\"messageType\":\"" + messageType + "\"}")
                .build();
    }

    public static GameEvent whisper(UUID gameId, UUID senderId, UUID receiverId,
            String senderRole, String content, String phase) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.WHISPER)
                .playerId(senderId)
                .targetPlayerId(receiverId)
                .playerRole(senderRole)
                .content(content)
                .phase(phase)
                .build();
    }

    public static GameEvent vote(UUID gameId, UUID voterId, UUID targetId,
            String voterRole, int round) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.VOTE_CAST)
                .playerId(voterId)
                .targetPlayerId(targetId)
                .playerRole(voterRole)
                .round(round)
                .build();
    }

    public static GameEvent phaseChange(UUID gameId, String fromPhase, String toPhase, int round) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.PHASE_CHANGE)
                .phase(toPhase)
                .round(round)
                .metadata("{\"fromPhase\":\"" + fromPhase + "\"}")
                .build();
    }

    public static GameEvent playerEliminated(UUID gameId, UUID playerId, String playerRole,
            String phase, String reason) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.PLAYER_ELIMINATED)
                .playerId(playerId)
                .playerRole(playerRole)
                .phase(phase)
                .metadata("{\"reason\":\"" + reason + "\"}")
                .build();
    }

    public static GameEvent gameStart(UUID gameId, int playerCount) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.GAME_START)
                .metadata("{\"playerCount\":" + playerCount + "}")
                .build();
    }

    public static GameEvent gameEnd(UUID gameId, String winningTeam, String metadata) {
        return GameEvent.builder()
                .gameId(gameId)
                .eventType(GameEventType.GAME_END)
                .content(winningTeam)
                .metadata(metadata)
                .build();
    }
}
