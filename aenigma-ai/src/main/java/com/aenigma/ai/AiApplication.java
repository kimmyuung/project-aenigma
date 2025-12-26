package com.aenigma.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 모듈 애플리케이션
 * 
 * GM 보조 기능 및 AI 학습 데이터 수집을 담당합니다.
 * 배치 스케줄링이 활성화되어 매일 새벽 3시에 학습 데이터를 처리합니다.
 */
@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
        "com.aenigma.ai.collector.entity",
        "com.aenigma.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.aenigma.ai.collector.repository",
        "com.aenigma.domain"
})
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
