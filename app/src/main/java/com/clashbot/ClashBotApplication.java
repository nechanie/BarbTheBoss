package com.clashbot;

import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClashBotApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ClashBotApplication.class, args);
        context.getBean(GatewayDiscordClient.class);
    }

    @Bean
    public Mono<Void> onShutdown(GatewayDiscordClient client) {
        // Graceful shutdown of the bot
        return client.onDisconnect();
    }
}
