package com.aenigma.socket.discord.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Discord Bot 설정
 */
@Configuration
@ConfigurationProperties(prefix = "discord")
@Getter
@Setter
public class DiscordProperties {

    private Bot bot = new Bot();

    @Getter
    @Setter
    public static class Bot {
        /**
         * Discord Bot Token
         */
        private String token;

        /**
         * Discord Guild (서버) ID
         */
        private String guildId;

        /**
         * Discord 연동 활성화 여부
         */
        private boolean enabled = false;
    }
}
