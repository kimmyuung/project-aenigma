package com.aenigma.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Aenigma API Application
 */
@SpringBootApplication(scanBasePackages = {
        "com.aenigma.api",
        "com.aenigma.domain"
})
@EntityScan("com.aenigma.domain.entity")
@EnableJpaRepositories("com.aenigma.domain.repository")
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class AenigmaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AenigmaApiApplication.class, args);
    }
}
