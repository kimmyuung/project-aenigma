package com.aenigma.socket.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WebSocket 서버용 보안 설정
 * 
 * 현재는 개발 단계이므로 모든 요청을 허용.
 * 추후 JWT 인증 및 STOMP 인터셉터 추가 예정.
 */
@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // WebSocket 엔드포인트 허용
                        .requestMatchers("/ws/**").permitAll()
                        // 기타 모든 요청 허용 (개발 단계)
                        .anyRequest().permitAll());

        return http.build();
    }
}
