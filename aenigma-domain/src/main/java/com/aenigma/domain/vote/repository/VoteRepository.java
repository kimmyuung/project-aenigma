package com.aenigma.domain.vote.repository;

import com.aenigma.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    /**
     * 특정 게임의 모든 투표 조회
     */
    List<Vote> findByGameId(UUID gameId);

    /**
     * 특정 플레이어의 투표 내역 조회 (중복 투표 방지용)
     */
    Optional<Vote> findByGameIdAndVoterId(UUID gameId, UUID voterId);

    /**
     * 최다 득표자 ID 조회 (동률 처리 로직은 Service에서 수행 권장)
     */
    @Query("SELECT v.target.id FROM Vote v WHERE v.game.id = :gameId GROUP BY v.target.id ORDER BY COUNT(v) DESC")
    List<UUID> findMostVotedPlayerIds(@Param("gameId") UUID gameId);
}
