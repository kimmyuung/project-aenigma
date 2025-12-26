package com.aenigma.socket.discord.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePhase;
import com.aenigma.domain.game.repository.GameRepository;
import com.aenigma.domain.room.entity.Room;
import com.aenigma.domain.room.entity.RoomMember;
import com.aenigma.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * Discord 슬래시 명령어 서비스 (GM 전용)
 */
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DiscordCommandService extends ListenerAdapter {

    private final DiscordBotService discordBotService;
    private final GameRepository gameRepository;

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
                        .addOption(OptionType.STRING, "action", "start | end", true),
                Commands.slash("phase", "페이즈 관리")
                        .addOption(OptionType.STRING, "action", "next | set", true)
                        .addOption(OptionType.STRING, "phase",
                                "INTRO | LOBBY | INVESTIGATION | FINAL_VOTE | CONCLUSION", false),
                Commands.slash("timer", "타이머 설정")
                        .addOption(OptionType.STRING, "type", "밀담 | 투표", true)
                        .addOption(OptionType.INTEGER, "minutes", "시간(분)", true),
                Commands.slash("announce", "안내 문구 전송")
                        .addOption(OptionType.STRING, "message", "안내 메시지", true),
                Commands.slash("config", "게임 설정")
                        .addOption(OptionType.STRING, "setting", "investigation-rounds", true)
                        .addOption(OptionType.INTEGER, "value", "값 (조사 라운드: 1~3)", true))
                .queue(
                        success -> log.info("Discord 슬래시 명령어 등록 완료"),
                        error -> log.error("Discord 슬래시 명령어 등록 실패", error));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // GM 권한 확인 (서버 관리자 또는 특정 역할)
        Member member = event.getMember();
        if (member == null || !isGm(member)) {
            event.reply("❌ GM만 사용할 수 있는 명령어입니다.").setEphemeral(true).queue();
            return;
        }

        String commandName = event.getName();

        switch (commandName) {
            case "game" -> handleGameCommand(event);
            case "phase" -> handlePhaseCommand(event);
            case "timer" -> handleTimerCommand(event);
            case "announce" -> handleAnnounceCommand(event);
            case "config" -> handleConfigCommand(event);
            default -> event.reply("알 수 없는 명령어입니다.").setEphemeral(true).queue();
        }
    }

    /**
     * GM 권한 확인 (서버 관리자 권한 보유자)
     */
    private boolean isGm(Member member) {
        // 서버 관리자 권한 또는 "GM" 역할 보유 확인
        return member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) ||
                member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase("GM"));
    }

    private void handleGameCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();

        switch (action.toLowerCase()) {
            case "start" -> {
                event.reply("🎮 게임이 시작되었습니다!").queue();
                log.info("GM {}이(가) 게임 시작 명령 실행", event.getUser().getName());
            }
            case "end" -> {
                event.reply("🏁 게임이 종료되었습니다!").queue();
                log.info("GM {}이(가) 게임 종료 명령 실행", event.getUser().getName());
            }
            default -> event.reply("❌ 사용법: /game start 또는 /game end").setEphemeral(true).queue();
        }
    }

    private void handlePhaseCommand(SlashCommandInteractionEvent event) {
        String action = event.getOption("action").getAsString();

        if ("next".equalsIgnoreCase(action)) {
            event.reply("➡️ 다음 페이즈로 진행합니다!").queue();
            log.info("GM {}이(가) 다음 페이즈 명령 실행", event.getUser().getName());
        } else if ("set".equalsIgnoreCase(action)) {
            var phaseOption = event.getOption("phase");
            if (phaseOption == null) {
                event.reply("❌ 페이즈를 지정해주세요: INTRO, LOBBY, INVESTIGATION, FINAL_VOTE, CONCLUSION").setEphemeral(true)
                        .queue();
                return;
            }
            String phase = phaseOption.getAsString();
            event.reply("📍 페이즈를 " + phase + "(으)로 설정합니다.").queue();
            log.info("GM {}이(가) 페이즈 설정: {}", event.getUser().getName(), phase);
        }
    }

    private void handleTimerCommand(SlashCommandInteractionEvent event) {
        String type = event.getOption("type").getAsString();
        int minutes = event.getOption("minutes").getAsInt();

        event.reply("⏱️ " + type + " 타이머가 " + minutes + "분으로 설정되었습니다.").queue();
        log.info("GM {}이(가) 타이머 설정: {} {}분", event.getUser().getName(), type, minutes);
    }

    private void handleAnnounceCommand(SlashCommandInteractionEvent event) {
        String message = event.getOption("message").getAsString();

        // 현재 채널에 안내 메시지 전송
        event.reply("📢 " + message).queue();
        log.info("GM {}이(가) 안내: {}", event.getUser().getName(), message);
    }

    private void handleConfigCommand(SlashCommandInteractionEvent event) {
        String setting = event.getOption("setting").getAsString();
        int value = event.getOption("value").getAsInt();

        switch (setting.toLowerCase()) {
            case "investigation-rounds" -> {
                if (value < 1 || value > 3) {
                    event.reply("❌ 조사 라운드는 1~3 사이여야 합니다.").setEphemeral(true).queue();
                    return;
                }
                event.reply("⚙️ 조사 라운드가 " + value + "회로 설정되었습니다.").queue();
                log.info("GM {}이(가) 조사 라운드 설정: {}회", event.getUser().getName(), value);
            }
            default -> event.reply("❌ 알 수 없는 설정입니다. 사용 가능: investigation-rounds").setEphemeral(true).queue();
        }
    }
}
