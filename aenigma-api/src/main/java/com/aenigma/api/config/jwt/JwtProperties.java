package com.aenigma.api.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티
 * application.yml에서 jwt.* 설정을 읽어옴
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * JWT 서명에 사용할 비밀키 (Base64 인코딩)
     */
    private String secretKey;

    /**
     * Access Token 만료 시간 (밀리초, 기본: 1시간)
     */
    private long accessTokenExpiration = 3600000;

    /**
     * Refresh Token 만료 시간 (밀리초, 기본: 7일)
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * Token 발급자
     */
    private String issuer = "aenigma";
}
