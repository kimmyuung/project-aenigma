package com.aenigma.api.game.dto;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GameRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 게임 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResponse {

    private UUID id;
    private UUID roomId;
    private int roundNumber;
    private GamePhase phase;
    private int dayCount;
    private GameRole winnerTeam;
    private List<PlayerInfo> players;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /**
     * Discord 서버 초대 링크 (음성 채팅 참여용)
     */
    private String discordInvite;

    /**
     * 플레이어 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlayerInfo {
        private UUID playerId;
        private UUID userId;
        private String nickname;
        private String displayName;
        private GameRole role; // 본인 또는 게임 종료 후에만 공개
        private boolean isAlive;
    }

    /**
     * 게임 정보 변환 (역할 비공개)
     */
    public static GameResponse from(Game game, UUID currentUserId) {
        return from(game, currentUserId, null);
    }

    /**
     * 게임 정보 변환 (Discord 초대 링크 포함)
     */
    public static GameResponse from(Game game, UUID currentUserId, String discordInvite) {
        List<PlayerInfo> playerInfos = game.getPlayers().stream()
                .map(player -> {
                    // 본인이거나 게임 종료 시에만 역할 공개
                    GameRole visibleRole = null;
                    if (game.getPhase() == GamePhase.FINISHED ||
                            player.getUser().getId().equals(currentUserId)) {
                        visibleRole = player.getRole();
                    }

                    return PlayerInfo.builder()
                            .playerId(player.getId())
                            .userId(player.getUser().getId())
                            .nickname(player.getUser().getNickname())
                            .displayName(player.getUser().getDisplayName())
                            .role(visibleRole)
                            .isAlive(player.getIsAlive())
                            .build();
                })
                .toList();

        return GameResponse.builder()
                .id(game.getId())
                .roomId(game.getRoom().getId())
                .roundNumber(game.getRoundNumber())
                .phase(game.getPhase())
                .dayCount(game.getDayCount())
                .winnerTeam(game.getWinnerTeam())
                .players(playerInfos)
                .startedAt(game.getStartedAt())
                .finishedAt(game.getFinishedAt())
                .discordInvite(discordInvite)
                .build();
    }
}
