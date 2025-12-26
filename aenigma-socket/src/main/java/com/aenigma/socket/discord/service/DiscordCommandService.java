package com.aenigma.socket.discord.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.service.GameService;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.service.RoomService;
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
                Commands.slash("bind", "채널에 방 바인딩")
                        .addOption(OptionType.STRING, "room-code", "방 코드", true))
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
     * 외부에서 게임 ID 매핑 조회
     */
    public UUID getActiveGameId(String channelId) {
        return channelGameMap.get(channelId);
    }
}
