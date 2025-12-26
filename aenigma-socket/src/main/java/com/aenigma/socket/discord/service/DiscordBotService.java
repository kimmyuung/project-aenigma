package com.aenigma.socket.discord.service;

import com.aenigma.socket.discord.config.DiscordProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Discord Bot 초기화 및 관리 서비스
 */
@Service
@ConditionalOnProperty(name = "discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DiscordBotService {

    private final DiscordProperties discordProperties;
    private JDA jda;

    @PostConstruct
    public void init() {
        try {
            String token = discordProperties.getBot().getToken();
            if (token == null || token.isBlank()) {
                log.warn("Discord Bot Token이 설정되지 않았습니다.");
                return;
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_VOICE_STATES,
                            GatewayIntent.GUILD_MEMBERS)
                    .build()
                    .awaitReady();

            log.info("Discord Bot 초기화 완료: {}", jda.getSelfUser().getName());
        } catch (Exception e) {
            log.error("Discord Bot 초기화 실패", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            log.info("Discord Bot 종료");
        }
    }

    public JDA getJda() {
        return jda;
    }

    public Guild getGuild() {
        if (jda == null)
            return null;
        String guildId = discordProperties.getBot().getGuildId();
        return jda.getGuildById(guildId);
    }

    public boolean isConnected() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }
}
