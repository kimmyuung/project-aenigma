package com.aenigma.api.game.dto;

import com.aenigma.domain.game.entity.GameClue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 단서 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClueResponse {

    private UUID id;
    private String title;
    private String content;
    private String clueType;
    private Integer revealRound;
    private int importance;
    private String imageUrl;
    private boolean isDiscovered;
    private String discoveredByNickname;

    public static ClueResponse from(GameClue clue) {
        return ClueResponse.builder()
                .id(clue.getId())
                .title(clue.getTitle())
                .content(clue.getContent())
                .clueType(clue.getClueType())
                .revealRound(clue.getRevealRound())
                .importance(clue.getImportance())
                .imageUrl(clue.getImageUrl())
                .isDiscovered(clue.getIsDiscovered())
                .discoveredByNickname(clue.getDiscoveredBy() != null
                        ? clue.getDiscoveredBy().getUser().getNickname()
                        : null)
                .build();
    }
}
