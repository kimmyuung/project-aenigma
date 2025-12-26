package com.aenigma.domain.vote.entity;

import com.aenigma.domain.common.entity.BaseTimeEntity;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 투표 엔티티
 * 
 * 게임 내 투표 기록을 저장합니다.
 */
@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_vote_game", columnList = "game_id"),
        @Index(name = "idx_vote_voter", columnList = "voter_id"),
        @Index(name = "idx_vote_target", columnList = "target_id")
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
     * 투표가 속한 게임
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * 투표자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private GamePlayer voter;

    /**
     * 투표 대상
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private GamePlayer target;

    /**
     * 투표 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    /**
     * 투표 라운드 (몇 번째 투표인지)
     */
    @Column(name = "round", nullable = false)
    private Integer round;

    // === Factory Methods ===

    public static Vote create(Game game, GamePlayer voter, GamePlayer target, VoteType voteType, int round) {
        return Vote.builder()
                .game(game)
                .voter(voter)
                .target(target)
                .voteType(voteType)
                .round(round)
                .build();
    }
}
