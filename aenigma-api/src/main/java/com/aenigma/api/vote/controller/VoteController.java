package com.aenigma.api.vote.controller;

import com.aenigma.api.game.dto.VoteRequest;
import com.aenigma.api.global.resolver.CurrentUserId;
import com.aenigma.api.vote.dto.VoteResponse;
import com.aenigma.api.vote.dto.VoteResultResponse;
import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 투표 API Controller
 */
@Tag(name = "Vote", description = "게임 투표 API")
@RestController
@RequestMapping("/api/games/{gameId}/votes")
@RequiredArgsConstructor
public class VoteController {

        private final VoteService voteService;
        private final GameRepository gameRepository;
        private final GamePlayerRepository gamePlayerRepository;

        /**
         * 투표 제출 (최종 투표)
         */
        @Operation(summary = "투표 제출", description = "게임에서 최종 투표를 제출합니다.")
        @PostMapping
        public ResponseEntity<VoteResponse> castVote(
                        @CurrentUserId UUID userId,
                        @PathVariable UUID gameId,
                        @Valid @RequestBody VoteRequest request) {

                Vote vote = voteService.vote(gameId, userId, request.getTargetPlayerId());

                return ResponseEntity.status(HttpStatus.CREATED).body(VoteResponse.from(vote));
        }

        /**
         * 투표 결과 조회
         */
        @Operation(summary = "투표 결과 조회", description = "해당 라운드의 투표 결과를 조회합니다.")
        @GetMapping("/results")
        public ResponseEntity<VoteResultResponse> getVoteResults(
                        @CurrentUserId UUID userId,
                        @PathVariable UUID gameId,
                        @RequestParam int round) {

                Game game = gameRepository.findById(gameId)
                                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

                Map<UUID, Long> results = voteService.getVoteResults(gameId, round);
                boolean isComplete = voteService.isVotingComplete(gameId, round);

                // 예상 투표수 계산
                long aliveCount = game.getPlayers().stream()
                                .filter(GamePlayer::getIsAlive)
                                .count();

                VoteResultResponse response = VoteResultResponse.builder()
                                .gameId(gameId)
                                .round(round)
                                .results(results)
                                .isComplete(isComplete)
                                .totalVotes(results.values().stream().mapToInt(Long::intValue).sum())
                                .expectedVotes((int) aliveCount)
                                .build();

                return ResponseEntity.ok(response);
        }

        /**
         * 투표 상태 조회
         */
        @Operation(summary = "투표 상태 조회", description = "현재 라운드의 투표 완료 여부를 확인합니다.")
        @GetMapping("/status")
        public ResponseEntity<Map<String, Object>> getVoteStatus(
                        @CurrentUserId UUID userId,
                        @PathVariable UUID gameId,
                        @RequestParam int round) {

                boolean isComplete = voteService.isVotingComplete(gameId, round);

                return ResponseEntity.ok(Map.of(
                                "gameId", gameId,
                                "round", round,
                                "isComplete", isComplete));
        }

        /**
         * 최다 득표자 조회
         */
        @Operation(summary = "최다 득표자 조회", description = "해당 라운드의 최다 득표자를 조회합니다.")
        @GetMapping("/top")
        public ResponseEntity<List<VoteResponse.PlayerInfo>> getMostVotedPlayers(
                        @CurrentUserId UUID userId,
                        @PathVariable UUID gameId,
                        @RequestParam int round) {

                List<GamePlayer> topPlayers = voteService.getMostVotedPlayers(gameId, round);
                List<VoteResponse.PlayerInfo> response = topPlayers.stream()
                                .map(p -> VoteResponse.PlayerInfo.builder()
                                                .playerId(p.getId())
                                                .userId(p.getUser().getId())
                                                .nickname(p.getUser().getNickname())
                                                .isAlive(p.getIsAlive())
                                                .build())
                                .toList();

                return ResponseEntity.ok(response);
        }

        /**
         * 내 투표 기록 조회
         */
        @Operation(summary = "내 투표 기록", description = "현재 게임에서 내가 한 투표 기록을 조회합니다.")
        @GetMapping("/my")
        public ResponseEntity<List<VoteResponse>> getMyVotes(
                        @CurrentUserId UUID userId,
                        @PathVariable UUID gameId) {

                GamePlayer player = findPlayerByGameAndUser(gameId, userId);
                List<Vote> votes = voteService.getPlayerVoteHistory(gameId, player.getId());
                List<VoteResponse> response = votes.stream()
                                .map(VoteResponse::from)
                                .toList();

                return ResponseEntity.ok(response);
        }

        // === Private Helper ===

        private GamePlayer findPlayerByGameAndUser(UUID gameId, UUID userId) {
                return gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                                .orElseThrow(() -> new IllegalArgumentException("게임에 참여하지 않은 사용자입니다."));
        }
}
