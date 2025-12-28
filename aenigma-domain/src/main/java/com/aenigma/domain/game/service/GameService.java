package com.aenigma.domain.game.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GameClue;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.entity.GameRole;
import com.aenigma.domain.game.repository.GameClueRepository;
import com.aenigma.domain.game.repository.GamePlayerRepository;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.scenario.entity.Scenario;
import com.aenigma.domain.scenario.entity.ScenarioClue;
import com.aenigma.domain.scenario.entity.ScenarioRole;
import com.aenigma.domain.scenario.repository.ScenarioRepository;
import com.aenigma.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 게임 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameClueRepository gameClueRepository;
    private final ScenarioRepository scenarioRepository;

    /**
     * 새 게임 생성
     */
    @Transactional
    public Game createGame(Room room) {
        long roundNumber = gameRepository.countByRoomId(room.getId()) + 1;

        Game game = Game.builder()
                .room(room)
                .roundNumber((int) roundNumber)
                .phase(GamePhase.INTRO)
                .dayCount(0)
                .build();

        return gameRepository.save(game);
    }

    /**
     * 시나리오 기반 게임 생성
     */
    @Transactional
    public Game createGameFromScenario(Room room, UUID scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오를 찾을 수 없습니다: " + scenarioId));

        long roundNumber = gameRepository.countByRoomId(room.getId()) + 1;

        Game game = Game.builder()
                .room(room)
                .scenario(scenario)
                .roundNumber((int) roundNumber)
                .phase(GamePhase.INTRO)
                .dayCount(0)
                .maxInvestigationRounds(scenario.getRecommendedRounds())
                .build();

        game = gameRepository.save(game);

        log.info("시나리오 기반 게임 생성: gameId={}, scenarioId={}", game.getId(), scenarioId);
        return game;
    }

    /**
     * 게임 생성 및 역할 배정 통합 메서드
     * 시나리오 ID가 있으면 시나리오 기반으로, 없으면 일반 게임으로 생성
     */
    @Transactional
    public Game createAndStartGame(Room room, UUID scenarioId) {
        Game game;
        if (scenarioId != null) {
            game = createGameFromScenario(room, scenarioId);
            assignRolesFromScenario(game, room.getMembers());
        } else {
            game = createGame(room);
            assignRoles(game, room.getMembers());
        }
        log.info("게임 생성 및 역할 배정 완료: gameId={}, scenarioId={}", game.getId(), scenarioId);
        return game;
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
     * 시나리오 역할 기반으로 플레이어에게 역할 배정
     */
    @Transactional
    public List<GamePlayer> assignRolesFromScenario(Game game, List<RoomMember> members) {
        Scenario scenario = game.getScenario();
        if (scenario == null) {
            throw new IllegalStateException("시나리오가 없는 게임입니다. 일반 역할 배정을 사용하세요.");
        }

        List<ScenarioRole> scenarioRoles = scenario.getRoles();
        if (members.size() > scenarioRoles.size()) {
            throw new IllegalArgumentException(
                    String.format("참가자 수(%d)가 시나리오 역할 수(%d)를 초과합니다.",
                            members.size(), scenarioRoles.size()));
        }

        // 역할 섞기
        List<ScenarioRole> shuffledRoles = new ArrayList<>(scenarioRoles);
        Collections.shuffle(shuffledRoles);

        List<GamePlayer> players = new ArrayList<>();
        Map<String, GamePlayer> roleNameToPlayerMap = new HashMap<>();

        for (int i = 0; i < members.size(); i++) {
            User user = members.get(i).getUser();
            ScenarioRole scenarioRole = shuffledRoles.get(i);

            GamePlayer player = GamePlayer.builder()
                    .game(game)
                    .user(user)
                    .role(scenarioRole.getRoleType())
                    .scenarioRoleName(scenarioRole.getName())
                    .scenarioRoleDescription(scenarioRole.getDescription())
                    .scenarioRoleSecret(scenarioRole.getSecretInfo())
                    .scenarioRoleObjective(scenarioRole.getObjective())
                    .isAlive(true)
                    .build();

            player = gamePlayerRepository.save(player);
            players.add(player);
            roleNameToPlayerMap.put(scenarioRole.getName(), player);
        }

        game.getPlayers().addAll(players);

        // 단서 초기화
        initializeCluesFromScenario(game, scenario, roleNameToPlayerMap);

        log.info("시나리오 역할 배정 완료: gameId={}, players={}", game.getId(), players.size());
        return players;
    }

    /**
     * 시나리오 단서를 게임에 초기화
     */
    private void initializeCluesFromScenario(Game game, Scenario scenario,
            Map<String, GamePlayer> roleNameToPlayerMap) {
        List<ScenarioClue> scenarioClues = scenario.getClues();

        for (ScenarioClue scenarioClue : scenarioClues) {
            GamePlayer assignedPlayer = null;

            // 개인 단서인 경우 해당 역할의 플레이어 찾기
            if (scenarioClue.getAssignedRole() != null) {
                String roleName = scenarioClue.getAssignedRole().getName();
                assignedPlayer = roleNameToPlayerMap.get(roleName);
            }

            GameClue gameClue = GameClue.from(scenarioClue, game, assignedPlayer);
            gameClueRepository.save(gameClue);
            game.getClues().add(gameClue);
        }

        log.info("단서 초기화 완료: gameId={}, clues={}", game.getId(), scenarioClues.size());
    }

    /**
     * 인원수에 따른 역할 목록 생성
     */
    private List<GameRole> generateRoleList(int playerCount) {
        List<GameRole> roles = new ArrayList<>();

        // 기본 구성: 범인 1명, 탐정 1명, 나머지 용의자
        roles.add(GameRole.CRIMINAL);

        if (playerCount > 1) {
            roles.add(GameRole.DETECTIVE);
        }

        for (int i = 2; i < playerCount; i++) {
            roles.add(GameRole.SUSPECT);
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

        game.nextPhase();
        game.checkWinCondition();

        return gameRepository.save(game);
    }

    /**
     * 플레이어 제거 (사망 처리)
     */
    @Transactional
    public GamePlayer eliminatePlayer(UUID gameId, UUID userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));

        player.eliminate();
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
     * 플레이어가 볼 수 있는 단서 목록 조회
     */
    public List<GameClue> getVisibleClues(UUID gameId, UUID userId) {
        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));

        return gameClueRepository.findVisibleClues(gameId, player.getId());
    }

    /**
     * 플레이어의 역할 상세 정보 조회
     */
    public GamePlayer getPlayerRole(UUID gameId, UUID userId) {
        return gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어를 찾을 수 없습니다."));
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

        Map<GameRole, Long> roleStats = new HashMap<>();
        for (GameRole role : GameRole.values()) {
            roleStats.put(role, gamePlayerRepository.countByUserIdAndRole(userId, role));
        }
        stats.put("roleStats", roleStats);

        return stats;
    }
}
