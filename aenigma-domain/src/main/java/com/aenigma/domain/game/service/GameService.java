package com.aenigma.domain.game.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 게임 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 새 게임 생성
     */
    @Transactional
    public Game createGame(Room room) {
        long roundNumber = gameRepository.countByRoomId(room.getId()) + 1;

        Game game = Game.builder()
                .room(room)
                .roundNumber((int) roundNumber)
                .phase(GamePhase.PREPARING)
                .dayCount(0)
                .build();

        return gameRepository.save(game);
    }

    /**
     * 게임에 플레이어 추가 및 역할 배정
     */
    @Transactional
    public List<GamePlayer> assignRoles(Game game, List<RoomMember> members) {
        List<GameRole> roles = generateRoleList(members.size());
        Collections.shuffle(roles);

        List<GamePlayer> players = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            User user = members.get(i).getUser();
            GameRole role = roles.get(i);

            GamePlayer player = GamePlayer.create(game, user, role);
            players.add(gamePlayerRepository.save(player));
        }

        game.getPlayers().addAll(players);
        return players;
    }

    /**
     * 인원수에 따른 역할 목록 생성
     */
    private List<GameRole> generateRoleList(int playerCount) {
        List<GameRole> roles = new ArrayList<>();

        // 기본 구성: 범인 1명, 탐정 1명, 나머지는 시민
        // 6명 이상: 범인 2명, 의사 1명 추가
        if (playerCount >= 6) {
            roles.add(GameRole.KILLER);
            roles.add(GameRole.KILLER);
            roles.add(GameRole.DETECTIVE);
            roles.add(GameRole.DOCTOR);
            for (int i = 4; i < playerCount; i++) {
                roles.add(GameRole.CITIZEN);
            }
        } else if (playerCount >= 4) {
            roles.add(GameRole.KILLER);
            roles.add(GameRole.DETECTIVE);
            for (int i = 2; i < playerCount; i++) {
                roles.add(GameRole.CITIZEN);
            }
        } else {
            // 최소 인원 (3명)
            roles.add(GameRole.KILLER);
            roles.add(GameRole.DETECTIVE);
            roles.add(GameRole.CITIZEN);
        }

        return roles;
    }

    /**
     * 게임 시작
     */
    @Transactional
    public Game startGame(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        game.start();
        return gameRepository.save(game);
    }

    /**
     * 다음 단계로 진행
     */
    @Transactional
    public Game nextPhase(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        // 다음 단계로 진행 전 플레이어 턴 초기화
        if (game.getPhase() == GamePhase.NIGHT) {
            game.getPlayers().forEach(GamePlayer::resetTurn);
        }

        game.nextPhase();

        // 승리 조건 체크
        game.checkWinCondition();

        return gameRepository.save(game);
    }

    /**
     * 플레이어 처형 (투표 결과)
     */
    @Transactional
    public GamePlayer executePlayer(UUID gameId, UUID userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));

        player.execute();
        return gamePlayerRepository.save(player);
    }

    /**
     * 플레이어 제거 (범인 행동)
     */
    @Transactional
    public GamePlayer eliminatePlayer(UUID gameId, UUID userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));

        player.eliminate();
        return gamePlayerRepository.save(player);
    }

    /**
     * 플레이어 보호 (의사 행동)
     */
    @Transactional
    public GamePlayer protectPlayer(UUID gameId, UUID userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));

        player.protect();
        return gamePlayerRepository.save(player);
    }

    /**
     * 게임 조회
     */
    public Optional<Game> findById(UUID gameId) {
        return gameRepository.findById(gameId);
    }

    /**
     * 방의 진행 중인 게임 조회
     */
    public Optional<Game> findActiveGameByRoomId(UUID roomId) {
        return gameRepository.findActiveGameByRoomId(roomId);
    }

    /**
     * 방의 게임 히스토리 조회
     */
    public List<Game> findGamesByRoomId(UUID roomId) {
        return gameRepository.findByRoomIdOrderByRoundNumberDesc(roomId);
    }

    /**
     * 사용자의 게임 통계 조회
     */
    public Map<String, Object> getUserStats(UUID userId) {
        Map<String, Object> stats = new HashMap<>();

        List<GamePlayer> allGames = gamePlayerRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long totalGames = allGames.size();
        long wins = gamePlayerRepository.countWinsByUserId(userId);

        stats.put("totalGames", totalGames);
        stats.put("wins", wins);
        stats.put("winRate", totalGames > 0 ? (double) wins / totalGames * 100 : 0);

        // 역할별 플레이 횟수
        Map<GameRole, Long> roleStats = new HashMap<>();
        for (GameRole role : GameRole.values()) {
            roleStats.put(role, gamePlayerRepository.countByUserIdAndRole(userId, role));
        }
        stats.put("roleStats", roleStats);

        return stats;
    }
}
