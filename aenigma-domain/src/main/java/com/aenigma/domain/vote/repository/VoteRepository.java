package com.aenigma.domain.vote.repository;

import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 투표 Repository
 */
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    /**
     * 특정 게임, 라운드의 모든 투표 조회
     */
    List<Vote> findByGameIdAndRound(UUID gameId, Integer round);

    /**
     * 특정 게임, 라운드, 투표 유형별 투표 조회
     */
    List<Vote> findByGameIdAndRoundAndVoteType(UUID gameId, Integer round, VoteType voteType);

    /**
     * 특정 투표자가 해당 라운드에 이미 투표했는지 확인
     */
    boolean existsByGameIdAndVoterIdAndRound(UUID gameId, UUID voterId, Integer round);

    /**
     * 특정 플레이어가 받은 투표 수 조회
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.game.id = :gameId AND v.round = :round AND v.target.id = :targetId")
    long countVotesForTarget(@Param("gameId") UUID gameId, @Param("round") int round, @Param("targetId") UUID targetId);

    /**
     * 라운드별 투표 결과 (가장 많은 표를 받은 플레이어)
     */
    @Query("SELECT v.target.id, COUNT(v) as voteCount FROM Vote v " +
            "WHERE v.game.id = :gameId AND v.round = :round " +
            "GROUP BY v.target.id ORDER BY voteCount DESC")
    List<Object[]> getVoteResultsByRound(@Param("gameId") UUID gameId, @Param("round") int round);

    /**
     * 특정 플레이어의 투표 기록 조회
     */
    List<Vote> findByGameIdAndVoterIdOrderByRoundAsc(UUID gameId, UUID voterId);
}
