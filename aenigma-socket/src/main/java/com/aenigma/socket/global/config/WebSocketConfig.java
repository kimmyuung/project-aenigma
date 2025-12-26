package com.aenigma.socket.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정
 * 
 * 엔드포인트:
 * - /ws : WebSocket 연결 엔드포인트 (SockJS 폴백 지원)
 * 
 * 메시지 브로커:
 * - /sub : 구독 prefix (서버 -> 클라이언트)
 * - /pub : 발행 prefix (클라이언트 -> 서버)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 prefix (서버 -> 클라이언트 메시지)
        // 예: /sub/chat/room/{roomId}, /sub/game/{gameId}
        registry.enableSimpleBroker("/sub");

        // 클라이언트가 메시지를 보낼 prefix (클라이언트 -> 서버)
        // 예: /pub/chat/message, /pub/game/action
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        // 클라이언트는 ws://localhost:8081/ws 로 연결
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 허용 (개발용, 프로덕션에서는 특정 도메인만)
                .withSockJS(); // SockJS 폴백 지원 (WebSocket 미지원 브라우저용)

        // SockJS 없이 순수 WebSocket만 사용하는 엔드포인트 (모바일 앱용)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
