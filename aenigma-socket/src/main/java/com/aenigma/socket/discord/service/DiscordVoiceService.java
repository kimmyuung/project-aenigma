package com.aenigma.socket.discord.service;

import com.aenigma.domain.game.entity.Game;
import com.aenigma.domain.game.entity.GamePlayer;
import com.aenigma.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Discord 음성 채널 관리 서비스
 */
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DiscordVoiceService {

    private final DiscordBotService discordBotService;

    private static final String GAME_CATEGORY_PREFIX = "🎮 Game-";
    private static final String LOBBY_CHANNEL_PREFIX = "🎮 Lobby-";
    private static final String WHISPER_CHANNEL_PREFIX = "🤫 Whisper-";
    private static final String GM_INQUIRY_PREFIX = "❓ GM문의-";

    /**
     * 게임용 채널 생성 (Lobby)
     */
    public CompletableFuture<VoiceChannel> createGameLobbyChannel(Game game) {
        Guild guild = discordBotService.getGuild();
        if (guild == null) {
            log.warn("Discord Guild를 찾을 수 없습니다.");
            return CompletableFuture.completedFuture(null);
        }

        String channelName = LOBBY_CHANNEL_PREFIX + game.getId().toString().substring(0, 8);

        return guild.createVoiceChannel(channelName)
                .submit()
                .whenComplete((channel, error) -> {
                    if (error != null) {
                        log.error("Lobby 채널 생성 실패: {}", error.getMessage());
                    } else {
                        log.info("Lobby 채널 생성 완료: {}", channel.getName());
                    }
                });
    }

    /**
     * 밀담용 Whisper 채널 생성
     */
    public CompletableFuture<VoiceChannel> createWhisperChannel(Game game, List<GamePlayer> players) {
        Guild guild = discordBotService.getGuild();
        if (guild == null) {
            return CompletableFuture.completedFuture(null);
        }

        String playerNames = players.stream()
                .map(p -> p.getUser().getNickname())
                .reduce((a, b) -> a + "-" + b)
                .orElse("unknown");

        String channelName = WHISPER_CHANNEL_PREFIX + playerNames;

        return guild.createVoiceChannel(channelName)
                .submit()
                .whenComplete((channel, error) -> {
                    if (error != null) {
                        log.error("Whisper 채널 생성 실패: {}", error.getMessage());
                    } else {
                        log.info("Whisper 채널 생성 완료: {}", channel.getName());
                    }
                });
    }

    /**
     * GM 문의용 채널 생성
     */
    public CompletableFuture<VoiceChannel> createGmInquiryChannel(User player) {
        Guild guild = discordBotService.getGuild();
        if (guild == null) {
            return CompletableFuture.completedFuture(null);
        }

        String channelName = GM_INQUIRY_PREFIX + player.getNickname();

        return guild.createVoiceChannel(channelName)
                .submit()
                .whenComplete((channel, error) -> {
                    if (error != null) {
                        log.error("GM 문의 채널 생성 실패: {}", error.getMessage());
                    } else {
                        log.info("GM 문의 채널 생성 완료: {}", channel.getName());
                    }
                });
    }

    /**
     * 플레이어를 특정 채널로 이동
     */
    public void movePlayerToChannel(User user, VoiceChannel channel) {
        Guild guild = discordBotService.getGuild();
        if (guild == null || user.getDiscordId() == null) {
            return;
        }

        Member member = guild.getMemberById(user.getDiscordId());
        if (member != null && member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
            guild.moveVoiceMember(member, channel).queue(
                    success -> log.debug("플레이어 이동 완료: {} -> {}", user.getNickname(), channel.getName()),
                    error -> log.error("플레이어 이동 실패: {}", error.getMessage()));
        }
    }

    /**
     * 채널 삭제
     */
    public void deleteChannel(VoiceChannel channel) {
        if (channel != null) {
            channel.delete().queue(
                    success -> log.info("채널 삭제 완료: {}", channel.getName()),
                    error -> log.error("채널 삭제 실패: {}", error.getMessage()));
        }
    }

    /**
     * 게임 종료 시 모든 게임 관련 채널 정리
     */
    public void cleanupGameChannels(Game game) {
        Guild guild = discordBotService.getGuild();
        if (guild == null)
            return;

        String gameIdPrefix = game.getId().toString().substring(0, 8);

        guild.getVoiceChannels().stream()
                .filter(ch -> ch.getName().contains(gameIdPrefix))
                .forEach(ch -> ch.delete().queue(
                        success -> log.debug("채널 정리: {}", ch.getName()),
                        error -> log.warn("채널 정리 실패: {}", ch.getName())));
    }
}
