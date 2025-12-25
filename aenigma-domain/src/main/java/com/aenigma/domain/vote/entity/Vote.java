package com.aenigma.domain.vote.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 최종 범인 지목 투표
 */
@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_vote_game", columnList = "game_id"),
        @Index(name = "idx_vote_voter", columnList = "voter_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_game_voter", columnNames = { "game_id", "voter_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Vote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 투표가 진행된 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 투표한 사람
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private GamePlayer voter;

    /**
     * 지목된 사람
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private GamePlayer target;

    /**
     * 지목 사유 (선택)
     */
    @Column(name = "reason")
    private String reason;
}
