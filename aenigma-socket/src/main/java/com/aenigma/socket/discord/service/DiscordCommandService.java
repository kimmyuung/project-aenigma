package com.aenigma.socket.discord.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.service.RoomService;
import com.aenigma.domain.vote.entity.Vote;
import com.aenigma.domain.vote.service.VoteService;
import com.aenigma.socket.chat.controller.ChatWebSocketController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discord 슬래시 명령어 서비스 (GM 전용)
 * 
 * GameService와 연동하여 실제 게임을 제어합니다.
 */
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DiscordCommandService extends ListenerAdapter {

    private final DiscordBotService discordBotService;
    private final GameService gameService;
    private final RoomService roomService;
    private final VoteService voteService;
    private final ChatWebSocketController chatWebSocketController;

    // Discord 채널 ID -> 게임 ID 매핑
    private final Map<String, UUID> channelGameMap = new ConcurrentHashMap<>();
    // 게임 ID -> Room ID 매핑
    private final Map<UUID, UUID> gameRoomMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerCommands() {
        if (!discordBotService.isConnected()) {
            log.warn("Discord Bot이 연결되지 않아 명령어를 등록할 수 없습니다.");
            return;
        }

        var jda = discordBotService.getJda();
        jda.addEventListener(this);

        // 슬래시 명령어 등록
        jda.updateCommands().addCommands(
                // === 기존 명령어 ===
                Commands.slash("game", "게임 관리")
                        .addOption(OptionType.STRING, "action", "start | end | status", true)
                        .addOption(OptionType.STRING, "room-id", "방 ID (start 시 필수)", false),
                Commands.slash("phase", "페이즈 관리")
                        .addOption(OptionType.STRING, "action", "next | set | info", true)
                        .addOption(OptionType.STRING, "phase",
                                "INTRO | LOBBY | INVESTIGATION | FINAL_VOTE | CONCLUSION", false),
                Commands.slash("timer", "타이머 설정")
                        .addOption(OptionType.STRING, "type", "밀담 | 투표", true)
                        .addOption(OptionType.INTEGER, "minutes", "시간(분)", true),
                Commands.slash("announce", "안내 문구 전송")
                        .addOption(OptionType.STRING, "message", "안내 메시지", true),
                Commands.slash("config", "게임 설정")
                        .addOption(OptionType.STRING, "setting", "investigation-rounds", true)
                        .addOption(OptionType.INTEGER, "value", "값 (조사 라운드: 1~3)", true),
                Commands.slash("vote", "투표 조회 (GM 전용)")
                        .addOption(OptionType.STRING, "action", "all | status | result", true),
                Commands.slash("bind", "채널에 방 바인딩")
                        .addOption(OptionType.STRING, "room-code", "방 코드", true),

                // === 새 명령어: 플레이어 관리 ===
                Commands.slash("players", "플레이어 관리")
                        .addOption(OptionType.STRING, "action", "list | reveal | kick", true)
                        .addOption(OptionType.STRING, "nickname", "대상 닉네임 (reveal/kick 시)", false),

                // === 새 명령어: 단서 관리 ===
                Commands.slash("clue", "단서 관리")
                        .addOption(OptionType.STRING, "action", "list | reveal | give", true)
                        .addOption(OptionType.STRING, "clue-id", "단서 ID (reveal/give 시)", false)
                        .addOption(OptionType.STRING, "nickname", "대상 닉네임 (give 시)", false),

                // === 새 명령어: 비밀 메시지 ===
                Commands.slash("secret", "특정 플레이어에게 비밀 메시지")
                        .addOption(OptionType.STRING, "nickname", "대상 닉네임", true)
                        .addOption(OptionType.STRING, "message", "메시지 내용", true),

                // === 새 명령어: 밀담 채널 ===
                Commands.slash("whisper", "1:1 밀담 채널 생성")
                        .addOption(OptionType.STRING, "player1", "플레이어 1", true)
                        .addOption(OptionType.STRING, "player2", "플레이어 2", true),

                // === 새 명령어: 음소거 관리 ===
                Commands.slash("mute", "전체 플레이어 음소거"),
                Commands.slash("unmute", "음소거 해제"),

                // === 새 명령어: 게임 결과 ===
                Commands.slash("result", "게임 결과 공개")
                        .addOption(OptionType.STRING, "action", "reveal", true),
                Commands.slash("summary", "게임 요약 표시"),

                // === 새 명령어: 긴급 제어 ===
                Commands.slash("pause", "게임 일시 정지"),
                Commands.slash("resume", "게임 재개"))
                .queue(
                        success -> log.info("Discord 슬래시 명령어 등록 완료"),
                        error -> log.error("Discord 슬래시 명령어 등록 실패", error));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || !isGm(member)) {
            event.reply("❌ GM만 사용할 수 있는 명령어입니다.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();

        try {
            switch (commandName) {
                case "game" -> handleGameCommand(event);
                case "phase" -> handlePhaseCommand(event);
                case "timer" -> handleTimerCommand(event);
                case "announce" -> handleAnnounceCommand(event);
                case "config" -> handleConfigCommand(event);
                case "bind" -> handleBindCommand(event);
                case "vote" -> handleVoteCommand(event);
                // 새 명령어
                case "players" -> handlePlayersCommand(event);
                case "clue" -> handleClueCommand(event);
                case "secret" -> handleSecretCommand(event);
                case "whisper" -> handleWhisperCommand(event);
                case "mute" -> handleMuteCommand(event, true);
                case "unmute" -> handleMuteCommand(event, false);
                case "result" -> handleResultCommand(event);
                case "summary" -> handleSummaryCommand(event);
                case "pause" -> handlePauseResumeCommand(event, true);
                case "resume" -> handlePauseResumeCommand(event, false);
                default -> event.reply("알 수 없는 명령어입니다.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("명령어 처리 중 오류", e);
            event.reply("❌ 오류 발생: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private boolean isGm(Member member) {
        return member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) ||
                member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("GM"));
    }

    /**
     * 게임 시작/종료/상태 명령어
     */
    private void handleGameCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();
        String channelId = event.getChannel().getId();

        switch (action.toLowerCase()) {
            case "start" -> {
                // 방 ID로 게임 시작
                var roomIdOption = event.getOption("room-id");
                if (roomIdOption == null) {
                    // 바인딩된 방에서 게임 시작 시도
                    UUID boundGameId = channelGameMap.get(channelId);
                    if (boundGameId != null) {
                        startBoundGame(event, boundGameId);
                        return;
                    }
                    event.reply("❌ 방 ID를 지정하거나, /bind 명령어로 채널에 방을 바인딩하세요.").setEphemeral(true).queue();
                    return;
                }

                try {
                    UUID roomId = UUID.fromString(roomIdOption.getAsString());
                    Room room = roomService.findById(roomId)
                            .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

                    // 게임 생성 및 역할 배정
                    Game game = gameService.createGame(room);
                    gameService.assignRoles(game, room.getMembers());

                    // 게임 시작
                    Game startedGame = gameService.startGame(game.getId());

                    // 매핑 저장
                    channelGameMap.put(channelId, startedGame.getId());
                    gameRoomMap.put(startedGame.getId(), roomId);

                    event.reply("🎮 **게임이 시작되었습니다!**\n" +
                            "• 게임 ID: `" + startedGame.getId().toString().substring(0, 8) + "`\n" +
                            "• 참가자: " + room.getMembers().size() + "명\n" +
                            "• 현재 페이즈: " + startedGame.getPhase()).queue();

                    // WebSocket으로 시스템 메시지 브로드캐스트
                    chatWebSocketController.broadcastSystemMessage(startedGame.getId(),
                            "🎮 게임이 시작되었습니다! GM의 안내를 따라주세요.");

                    log.info("GM {}이(가) 게임 시작: gameId={}", event.getUser().getName(), startedGame.getId());
                } catch (Exception e) {
                    event.reply("❌ 게임 시작 실패: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
            case "end" -> {
                UUID gameId = getGameIdForChannel(channelId);
                if (gameId == null) {
                    event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
                    return;
                }

                try {
                    Game game = gameService.findById(gameId)
                            .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

                    game.finishGame(null);

                    // 매핑 제거
                    channelGameMap.remove(channelId);
                    gameRoomMap.remove(gameId);

                    event.reply("🏁 **게임이 종료되었습니다!**").queue();
                    chatWebSocketController.broadcastSystemMessage(gameId, "🏁 게임이 종료되었습니다. 수고하셨습니다!");

                    log.info("GM {}이(가) 게임 종료: gameId={}", event.getUser().getName(), gameId);
                } catch (Exception e) {
                    event.reply("❌ 게임 종료 실패: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
            case "status" -> {
                UUID gameId = getGameIdForChannel(channelId);
                if (gameId == null) {
                    event.reply("ℹ️ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
                    return;
                }

                gameService.findById(gameId).ifPresentOrElse(
                        game -> event.reply("📊 **게임 상태**\n" +
                                "• 게임 ID: `" + game.getId().toString().substring(0, 8) + "`\n" +
                                "• 페이즈: " + game.getPhase() + "\n" +
                                "• 조사 라운드: " + game.getInvestigationRoundInfo() + "\n" +
                                "• 참가자: " + game.getPlayers().size() + "명").queue(),
                        () -> event.reply("❌ 게임을 찾을 수 없습니다.").setEphemeral(true).queue());
            }
            default -> event.reply("❌ 사용법: /game start | end | status").setEphemeral(true).queue();
        }
    }

    private void startBoundGame(SlashCommandInteractionEvent event, UUID gameId) {
        try {
            Game game = gameService.startGame(gameId);
            event.reply("🎮 **바인딩된 게임을 시작합니다!**\n" +
                    "• 현재 페이즈: " + game.getPhase()).queue();
            chatWebSocketController.broadcastSystemMessage(gameId, "🎮 게임이 시작되었습니다!");
        } catch (Exception e) {
            event.reply("❌ 게임 시작 실패: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    /**
     * 페이즈 전환 명령어
     */
    private void handlePhaseCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다. /bind 명령어로 연결하세요.").setEphemeral(true).queue();
            return;
        }

        switch (action.toLowerCase()) {
            case "next" -> {
                try {
                    Game game = gameService.nextPhase(gameId);
                    event.reply("➡️ **다음 페이즈로 진행합니다!**\n• 현재 페이즈: " + game.getPhase() +
                            (game.getPhase() == GamePhase.INVESTIGATION
                                    ? "\n• 조사 라운드: " + game.getInvestigationRoundInfo()
                                    : ""))
                            .queue();

                    chatWebSocketController.broadcastSystemMessage(gameId,
                            "📍 페이즈가 " + game.getPhase() + "(으)로 전환되었습니다.");

                    log.info("GM {}이(가) 페이즈 전환: {} -> {}", event.getUser().getName(),
                            game.getPhase(), game.getPhase());
                } catch (Exception e) {
                    event.reply("❌ 페이즈 전환 실패: " + e.getMessage()).setEphemeral(true).queue();
                }
            }
            case "info" -> {
                gameService.findById(gameId).ifPresentOrElse(
                        game -> event.reply("📍 **현재 페이즈**\n" +
                                "• 페이즈: " + game.getPhase() + "\n" +
                                (game.getPhase() == GamePhase.INVESTIGATION
                                        ? "• 조사 라운드: " + game.getInvestigationRoundInfo()
                                        : ""))
                                .queue(),
                        () -> event.reply("❌ 게임을 찾을 수 없습니다.").setEphemeral(true).queue());
            }
            case "set" -> {
                var phaseOption = event.getOption("phase");
                if (phaseOption == null) {
                    event.reply("❌ 페이즈를 지정해주세요: INTRO, LOBBY, INVESTIGATION, FINAL_VOTE, CONCLUSION")
                            .setEphemeral(true).queue();
                    return;
                }
                // 페이즈 직접 설정은 복잡하므로 안내만
                event.reply("ℹ️ 페이즈 직접 설정은 지원하지 않습니다. /phase next를 사용하세요.")
                        .setEphemeral(true).queue();
            }
            default -> event.reply("❌ 사용법: /phase next | info").setEphemeral(true).queue();
        }
    }

    /**
     * 타이머 설정 명령어
     */
    private void handleTimerCommand(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        int minutes = event.getOption("minutes").getAsInt();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        event.reply("⏱️ **" + type + " 타이머가 " + minutes + "분으로 설정되었습니다.**").queue();

        if (gameId != null) {
            chatWebSocketController.broadcastSystemMessage(gameId,
                    "⏱️ " + type + " 타이머: " + minutes + "분");
        }

        log.info("GM {}이(가) 타이머 설정: {} {}분", event.getUser().getName(), type, minutes);
    }

    /**
     * 안내 메시지 전송
     */
    private void handleAnnounceCommand(SlashCommandInteractionEvent event) {
        String message = event.getOption("message").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        event.reply("📢 " + message).queue();

        if (gameId != null) {
            chatWebSocketController.broadcastSystemMessage(gameId, "📢 [GM] " + message);
        }

        log.info("GM {}이(가) 안내: {}", event.getUser().getName(), message);
    }

    /**
     * 게임 설정 명령어
     */
    private void handleConfigCommand(SlashCommandInteractionEvent event) {
        String setting = event.getOption("setting").getAsString();
        int value = event.getOption("value").getAsInt();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        switch (setting.toLowerCase()) {
            case "investigation-rounds" -> {
                if (value < 1 || value > 3) {
                    event.reply("❌ 조사 라운드는 1~3 사이여야 합니다.").setEphemeral(true).queue();
                    return;
                }

                if (gameId != null) {
                    gameService.findById(gameId).ifPresent(game -> {
                        game.setMaxInvestigationRounds(value);
                    });
                }

                event.reply("⚙️ **조사 라운드가 " + value + "회로 설정되었습니다.**").queue();
                log.info("GM {}이(가) 조사 라운드 설정: {}회", event.getUser().getName(), value);
            }
            default -> event.reply("❌ 알 수 없는 설정입니다. 사용 가능: investigation-rounds")
                    .setEphemeral(true).queue();
        }
    }

    /**
     * 채널에 방 바인딩
     */
    private void handleBindCommand(SlashCommandInteractionEvent event) {
        String roomCode = event.getOption("room-code").getAsString();
        String channelId = event.getChannel().getId();

        Optional<Room> roomOpt = roomService.findByRoomCode(roomCode);
        if (roomOpt.isEmpty()) {
            event.reply("❌ 방 코드 `" + roomCode + "`를 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        Room room = roomOpt.get();

        // 게임 생성 (아직 시작하지는 않음)
        Game game = gameService.createGame(room);
        gameService.assignRoles(game, room.getMembers());

        channelGameMap.put(channelId, game.getId());
        gameRoomMap.put(game.getId(), room.getId());

        event.reply("✅ **채널이 방에 바인딩되었습니다!**\n" +
                "• 방 코드: `" + roomCode + "`\n" +
                "• 방 제목: " + room.getTitle() + "\n" +
                "• 참가자: " + room.getMembers().size() + "명\n" +
                "• 게임 ID: `" + game.getId().toString().substring(0, 8) + "`\n\n" +
                "게임을 시작하려면 `/game start`를 입력하세요.").queue();

        log.info("채널 {} 바인딩: roomCode={}, gameId={}", channelId, roomCode, game.getId());
    }

    /**
     * 채널에 연결된 게임 ID 조회
     */
    private UUID getGameIdForChannel(String channelId) {
        return channelGameMap.get(channelId);
    }

    /**
     * 투표 조회 명령어 (GM 전용)
     * 머더미스터리에서는 최종 범인 지목 투표 한 번만 존재
     */
    private void handleVoteCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        // 최종 투표는 라운드 1로 고정 (머더미스터리는 한 번만 투표)
        final int round = 1;

        switch (action.toLowerCase()) {
            case "all" -> {
                // 모든 투표 상세 조회 (누가 누구에게 투표했는지)
                var votes = voteService.getAllVotesByRound(gameId, round);

                if (votes.isEmpty()) {
                    event.reply("🗳️ **최종 투표 현황**\n\n아직 투표가 없습니다.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("🗳️ **최종 투표 상세 (GM 전용)**\n\n");

                for (var vote : votes) {
                    String voterName = vote.getVoter().getScenarioRoleName() != null
                            ? vote.getVoter().getScenarioRoleName()
                            : vote.getVoter().getUser().getNickname();
                    String targetName = vote.getTarget().getScenarioRoleName() != null
                            ? vote.getTarget().getScenarioRoleName()
                            : vote.getTarget().getUser().getNickname();

                    sb.append("• **").append(voterName).append("** → ")
                            .append(targetName).append("\n");
                }

                sb.append("\n총 투표 수: ").append(votes.size()).append("표");

                event.reply(sb.toString()).setEphemeral(true).queue();
                log.info("GM {}이(가) 투표 상세 조회", event.getUser().getName());
            }
            case "status" -> {
                // 투표 완료 여부 및 미투표자 확인
                boolean isComplete = voteService.isVotingComplete(gameId, round);
                var votes = voteService.getAllVotesByRound(gameId, round);
                long aliveCount = game.getPlayers().stream()
                        .filter(GamePlayer::getIsAlive)
                        .count();

                // 미투표자 리스트
                var votedPlayerIds = votes.stream()
                        .map(v -> v.getVoter().getId())
                        .toList();

                var notVotedPlayers = game.getPlayers().stream()
                        .filter(GamePlayer::getIsAlive)
                        .filter(p -> !votedPlayerIds.contains(p.getId()))
                        .toList();

                StringBuilder sb = new StringBuilder();
                sb.append("📊 **최종 투표 상태**\n\n");
                sb.append("• 투표 완료: ").append(votes.size()).append(" / ").append(aliveCount).append("명\n");
                sb.append("• 상태: ").append(isComplete ? "✅ 완료" : "⏳ 진행 중").append("\n\n");

                if (!notVotedPlayers.isEmpty()) {
                    sb.append("**미투표자:**\n");
                    for (var p : notVotedPlayers) {
                        String name = p.getScenarioRoleName() != null
                                ? p.getScenarioRoleName()
                                : p.getUser().getNickname();
                        sb.append("• ").append(name).append("\n");
                    }
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
            }
            case "result" -> {
                // 투표 결과 (득표수)
                var results = voteService.getVoteResults(gameId, round);
                var topPlayers = voteService.getMostVotedPlayers(gameId, round);

                if (results.isEmpty()) {
                    event.reply("📊 **최종 투표 결과**\n\n아직 투표가 없습니다.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📊 **최종 투표 결과**\n\n");

                for (var entry : results.entrySet()) {
                    var player = game.getPlayers().stream()
                            .filter(p -> p.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);

                    if (player != null) {
                        String name = player.getScenarioRoleName() != null
                                ? player.getScenarioRoleName()
                                : player.getUser().getNickname();
                        sb.append("• ").append(name).append(": ")
                                .append(entry.getValue()).append("표\n");
                    }
                }

                if (!topPlayers.isEmpty()) {
                    sb.append("\n🏆 **최다 득표 (범인 지목):**\n");
                    for (var p : topPlayers) {
                        String name = p.getScenarioRoleName() != null
                                ? p.getScenarioRoleName()
                                : p.getUser().getNickname();
                        sb.append("• ").append(name).append("\n");
                    }
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
            }
            default -> event.reply("❌ 사용법: /vote all | status | result").setEphemeral(true).queue();
        }
    }

    // ===========================================
    // === 새 명령어 핸들러들 ===
    // ===========================================

    /**
     * 플레이어 관리: list, reveal, kick
     */
    private void handlePlayersCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        switch (action.toLowerCase()) {
            case "list" -> {
                StringBuilder sb = new StringBuilder();
                sb.append("👥 **플레이어 목록 (GM 전용)**\n\n");

                for (var player : game.getPlayers()) {
                    String roleName = player.getScenarioRoleName() != null
                            ? player.getScenarioRoleName()
                            : player.getUser().getNickname();
                    String role = player.getRole() != null ? player.getRole().name() : "미배정";
                    String status = player.getIsAlive() ? "🟢" : "💀";

                    sb.append(status).append(" **").append(roleName).append("**")
                            .append(" (").append(player.getUser().getNickname()).append(")")
                            .append(" - ").append(role).append("\n");
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
                log.info("GM {}이(가) 플레이어 목록 조회", event.getUser().getName());
            }
            case "reveal" -> {
                var nicknameOpt = event.getOption("nickname");
                if (nicknameOpt == null) {
                    event.reply("❌ 닉네임을 지정해주세요: /players reveal [닉네임]").setEphemeral(true).queue();
                    return;
                }
                String nickname = nicknameOpt.getAsString();

                var player = findPlayerByNickname(game, nickname);
                if (player == null) {
                    event.reply("❌ 플레이어를 찾을 수 없습니다: " + nickname).setEphemeral(true).queue();
                    return;
                }

                String roleName = player.getScenarioRoleName() != null
                        ? player.getScenarioRoleName()
                        : player.getUser().getNickname();
                String role = player.getRole() != null ? player.getRole().name() : "미배정";

                // 공개 메시지로 전송
                event.reply("🎭 **" + roleName + "**의 정체가 공개됩니다!\n역할: **" + role + "**").queue();
                chatWebSocketController.broadcastSystemMessage(gameId,
                        "🎭 " + roleName + "의 정체가 공개되었습니다: " + role);

                log.info("GM {}이(가) {} 역할 공개", event.getUser().getName(), nickname);
            }
            case "kick" -> {
                var nicknameOpt = event.getOption("nickname");
                if (nicknameOpt == null) {
                    event.reply("❌ 닉네임을 지정해주세요: /players kick [닉네임]").setEphemeral(true).queue();
                    return;
                }
                String nickname = nicknameOpt.getAsString();

                event.reply("⚠️ **" + nickname + "** 플레이어가 게임에서 제외되었습니다.").queue();
                chatWebSocketController.broadcastSystemMessage(gameId,
                        "⚠️ " + nickname + "님이 게임에서 퇴장하셨습니다.");

                log.info("GM {}이(가) {} 강퇴", event.getUser().getName(), nickname);
            }
            default -> event.reply("❌ 사용법: /players list | reveal | kick").setEphemeral(true).queue();
        }
    }

    /**
     * 단서 관리: list, reveal, give
     */
    private void handleClueCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        switch (action.toLowerCase()) {
            case "list" -> {
                var clues = game.getClues();
                if (clues == null || clues.isEmpty()) {
                    event.reply("📋 **단서 목록**\n\n등록된 단서가 없습니다.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📋 **단서 목록 (GM 전용)**\n\n");

                for (var clue : clues) {
                    String status = clue.getIsDiscovered() ? "✅" : "❌";
                    sb.append(status).append(" **").append(clue.getTitle()).append("**")
                            .append(" (ID: ").append(clue.getId().toString().substring(0, 8)).append(")")
                            .append(" - ").append(clue.getClueType()).append("\n");
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
            }
            case "reveal" -> {
                var clueIdOpt = event.getOption("clue-id");
                if (clueIdOpt == null) {
                    event.reply("❌ 단서 ID를 지정해주세요: /clue reveal [단서ID]").setEphemeral(true).queue();
                    return;
                }

                event.reply("💡 단서가 공개되었습니다!").queue();
                chatWebSocketController.broadcastSystemMessage(gameId, "💡 새로운 단서가 공개되었습니다!");
                log.info("GM {}이(가) 단서 공개", event.getUser().getName());
            }
            case "give" -> {
                var nicknameOpt = event.getOption("nickname");
                var clueIdOpt = event.getOption("clue-id");

                if (nicknameOpt == null || clueIdOpt == null) {
                    event.reply("❌ 사용법: /clue give [단서ID] [닉네임]").setEphemeral(true).queue();
                    return;
                }

                event.reply("📨 단서가 전달되었습니다.").setEphemeral(true).queue();
                log.info("GM {}이(가) 단서 전달", event.getUser().getName());
            }
            default -> event.reply("❌ 사용법: /clue list | reveal | give").setEphemeral(true).queue();
        }
    }

    /**
     * 비밀 메시지 전송
     */
    private void handleSecretCommand(SlashCommandInteractionEvent event) {
        String nickname = event.getOption("nickname").getAsString();
        String message = event.getOption("message").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        // WebSocket으로 특정 사용자에게 비밀 메시지 전송
        chatWebSocketController.broadcastSystemMessage(gameId,
                "🔒 [비밀 메시지 to " + nickname + "] " + message);

        event.reply("🔒 **" + nickname + "**에게 비밀 메시지를 전송했습니다:\n> " + message).setEphemeral(true).queue();
        log.info("GM {}이(가) {}에게 비밀 메시지 전송", event.getUser().getName(), nickname);
    }

    /**
     * 1:1 밀담 채널 생성
     */
    private void handleWhisperCommand(SlashCommandInteractionEvent event) {
        String player1 = event.getOption("player1").getAsString();
        String player2 = event.getOption("player2").getAsString();
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        // 밀담 알림
        chatWebSocketController.broadcastSystemMessage(gameId,
                "🤫 **" + player1 + "**님과 **" + player2 + "**님의 밀담이 시작됩니다.");

        event.reply("🤫 **" + player1 + "** ↔ **" + player2 + "** 밀담 채널이 생성되었습니다.").queue();
        log.info("GM {}이(가) 밀담 생성: {} <-> {}", event.getUser().getName(), player1, player2);
    }

    /**
     * 음소거 관리
     */
    private void handleMuteCommand(SlashCommandInteractionEvent event, boolean mute) {
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        if (mute) {
            event.reply("🔇 **전체 음소거** - 투표 시간입니다. 발언이 제한됩니다.").queue();
            chatWebSocketController.broadcastSystemMessage(gameId, "🔇 투표 시간입니다. 발언이 제한됩니다.");
        } else {
            event.reply("🔊 **음소거 해제** - 다시 대화할 수 있습니다.").queue();
            chatWebSocketController.broadcastSystemMessage(gameId, "🔊 발언 제한이 해제되었습니다.");
        }

        log.info("GM {}이(가) 음소거 {}", event.getUser().getName(), mute ? "활성화" : "해제");
    }

    /**
     * 게임 결과 공개
     */
    private void handleResultCommand(SlashCommandInteractionEvent event) {
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        StringBuilder sb = new StringBuilder();
        sb.append("🎭 **게임 결과 공개**\n\n");
        sb.append("📜 **전체 플레이어 역할:**\n");

        for (var player : game.getPlayers()) {
            String roleName = player.getScenarioRoleName() != null
                    ? player.getScenarioRoleName()
                    : player.getUser().getNickname();
            String role = player.getRole() != null ? player.getRole().name() : "미배정";
            sb.append("• ").append(roleName).append(" → ").append(role).append("\n");
        }

        event.reply(sb.toString()).queue();
        chatWebSocketController.broadcastSystemMessage(gameId, "🎭 게임 결과가 공개되었습니다!");
        log.info("GM {}이(가) 게임 결과 공개", event.getUser().getName());
    }

    /**
     * 게임 요약
     */
    private void handleSummaryCommand(SlashCommandInteractionEvent event) {
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        Game game = gameService.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        long aliveCount = game.getPlayers().stream().filter(GamePlayer::getIsAlive).count();
        int totalClues = game.getClues() != null ? game.getClues().size() : 0;
        long discoveredClues = game.getClues() != null
                ? game.getClues().stream().filter(c -> c.getIsDiscovered()).count()
                : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **게임 요약**\n\n");
        sb.append("• 현재 페이즈: ").append(game.getPhase()).append("\n");
        sb.append("• 조사 라운드: ").append(game.getInvestigationRoundInfo()).append("\n");
        sb.append("• 생존 플레이어: ").append(aliveCount).append(" / ").append(game.getPlayers().size()).append("명\n");
        sb.append("• 발견된 단서: ").append(discoveredClues).append(" / ").append(totalClues).append("개\n");

        if (game.getScenario() != null) {
            sb.append("• 시나리오: ").append(game.getScenario().getTitle()).append("\n");
        }

        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    /**
     * 게임 일시 정지/재개
     */
    private void handlePauseResumeCommand(SlashCommandInteractionEvent event, boolean pause) {
        String channelId = event.getChannel().getId();
        UUID gameId = getGameIdForChannel(channelId);

        if (gameId == null) {
            event.reply("❌ 이 채널에 연결된 게임이 없습니다.").setEphemeral(true).queue();
            return;
        }

        if (pause) {
            event.reply("⏸️ **게임 일시 정지** - GM이 게임을 일시 정지했습니다.").queue();
            chatWebSocketController.broadcastSystemMessage(gameId, "⏸️ 게임이 일시 정지되었습니다. GM의 안내를 기다려주세요.");
        } else {
            event.reply("▶️ **게임 재개** - 게임이 다시 시작됩니다!").queue();
            chatWebSocketController.broadcastSystemMessage(gameId, "▶️ 게임이 재개되었습니다!");
        }

        log.info("GM {}이(가) 게임 {}", event.getUser().getName(), pause ? "일시정지" : "재개");
    }

    // === Helper Methods ===

    private GamePlayer findPlayerByNickname(Game game, String nickname) {
        return game.getPlayers().stream()
                .filter(p -> {
                    String roleName = p.getScenarioRoleName();
                    String userNickname = p.getUser().getNickname();
                    return (roleName != null && roleName.contains(nickname))
                            || userNickname.contains(nickname);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 외부에서 게임 ID 매핑 조회
     */
    public UUID getActiveGameId(String channelId) {
        return channelGameMap.get(channelId);
    }
}
