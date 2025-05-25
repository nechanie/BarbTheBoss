package com.clashbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${api.base.url}")
    private String baseUrl;

    @Value("${deployment.type}")
    private String deploymentType;

    @Bean
    public WebClient clashbotApiClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            // only install the filter if we’re _not_ in prod
            .filter((request, next) -> {
            if (!"prod".equalsIgnoreCase(deploymentType)) {
                log.info("[API ⇒]  " + request.method() + " " + request.url());
            }
            return next.exchange(request)
                        .doOnNext(response -> {
                        if (!"prod".equalsIgnoreCase(deploymentType)) {
                            System.out.println("[API ⇐] " + request.method() + " " + request.url() + " → " + response.statusCode().value());
                        }
                        });
            })
            .build();
    }
}