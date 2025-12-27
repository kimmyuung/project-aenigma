package com.aenigma.domain.vote.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.entity.VoteType;
import com.aenigma.domain.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 투표 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 투표 생성
     */
    @Transactional
    public Vote castVote(UUID gameId, UUID voterId, UUID targetId, VoteType voteType, int round) {
        // 이미 투표했는지 확인
        if (voteRepository.existsByGameIdAndVoterIdAndRound(gameId, voterId, round)) {
            throw new IllegalStateException("이미 이번 라운드에 투표하셨습니다.");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        GamePlayer voter = gamePlayerRepository.findById(voterId)
                .orElseThrow(() -> new IllegalArgumentException("투표자를 찾을 수 없습니다."));

        GamePlayer target = gamePlayerRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("투표 대상을 찾을 수 없습니다."));

        // 생존자만 투표 가능
        if (!voter.getIsAlive()) {
            throw new IllegalStateException("사망한 플레이어는 투표할 수 없습니다.");
        }

        // 생존자만 투표 대상 가능
        if (!target.getIsAlive()) {
            throw new IllegalArgumentException("사망한 플레이어에게 투표할 수 없습니다.");
        }

        Vote vote = Vote.create(game, voter, target, voteType, round);
        Vote savedVote = voteRepository.save(vote);

        log.info("투표 완료: voter={}, target={}, type={}, round={}",
                voter.getUser().getNickname(), target.getUser().getNickname(), voteType, round);

        return savedVote;
    }

    /**
     * 간소화된 투표 메서드 (API 컨트롤러용)
     * 현재 조사 라운드로 FINAL 투표를 생성합니다.
     */
    @Transactional
    public Vote vote(UUID gameId, UUID userId, UUID targetPlayerId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        // userId로부터 해당 게임의 플레이어 찾기
        GamePlayer voter = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("이 게임의 플레이어가 아닙니다."));

        return castVote(gameId, voter.getId(), targetPlayerId, VoteType.FINAL_VOTE, game.getInvestigationRound());
    }

    /**
     * 투표 결과 집계
     * 
     * @return Map<플레이어ID, 득표수>
     */
    public Map<UUID, Long> getVoteResults(UUID gameId, int round) {
        List<Object[]> results = voteRepository.getVoteResultsByRound(gameId, round);
        Map<UUID, Long> voteResults = new LinkedHashMap<>();

        for (Object[] result : results) {
            UUID targetId = (UUID) result[0];
            Long voteCount = (Long) result[1];
            voteResults.put(targetId, voteCount);
        }

        return voteResults;
    }

    /**
     * 최다 득표자 조회 (동점일 경우 여러 명 반환)
     */
    public List<GamePlayer> getMostVotedPlayers(UUID gameId, int round) {
        Map<UUID, Long> results = getVoteResults(gameId, round);

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // 최다 득표수 찾기
        long maxVotes = results.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        // 최다 득표자 목록
        List<UUID> topVotedIds = results.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        return topVotedIds.stream()
                .map(id -> gamePlayerRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 라운드 투표 완료 여부 확인
     */
    public boolean isVotingComplete(UUID gameId, int round) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        long alivePlayerCount = game.getPlayers().stream()
                .filter(GamePlayer::getIsAlive)
                .count();

        List<Vote> votes = voteRepository.findByGameIdAndRound(gameId, round);

        return votes.size() >= alivePlayerCount;
    }

    /**
     * 플레이어별 투표 기록 조회
     */
    public List<Vote> getPlayerVoteHistory(UUID gameId, UUID playerId) {
        return voteRepository.findByGameIdAndVoterIdOrderByRoundAsc(gameId, playerId);
    }

    /**
     * 라운드별 모든 투표 상세 조회 (GM 전용)
     */
    public List<Vote> getAllVotesByRound(UUID gameId, int round) {
        return voteRepository.findByGameIdAndRound(gameId, round);
    }
}
