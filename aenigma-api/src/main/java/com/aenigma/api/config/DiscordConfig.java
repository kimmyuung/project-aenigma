package com.aenigma.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Discord 관련 설정 (API 모듈용)
 * 게임 시작 시 플레이어에게 Discord 초대 링크를 제공하기 위한 설정
 */
@Configuration
@ConfigurationProperties(prefix = "discord")
@Getter
@Setter
public class DiscordConfig {

    private Bot bot = new Bot();

    @Getter
    @Setter
    public static class Bot {
        /**
         * Discord 서버 초대 링크
         * 게임 시작 시 플레이어에게 표시됩니다.
         * 예: https://discord.gg/xxxxx
         */
        private String inviteLink;

        /**
         * Discord 연동 활성화 여부
         */
        private boolean enabled = false;
    }
}
