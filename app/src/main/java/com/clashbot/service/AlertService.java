package com.clashbot.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.AlertEmbeds.AlertEmbeddingBuilder;
import com.clashbot.models.Alert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AlertService {
    
    private final GatewayDiscordClient client;
    private final ClashBotApiClient clashBotApiClient;

    public AlertService(GatewayDiscordClient client, ClashBotApiClient clashBotApiClient){
        this.client = client;
        this.clashBotApiClient = clashBotApiClient;
    }

    /**
     * Send a notification based on the alert type.
     */
    public <T extends Alert> Mono<Void> sendAlert(T payload, Function<T, Mono<EmbedCreateSpec>> embedCreator) {
        return Flux.fromIterable(payload.getServerClans())
            .flatMap(serverClan ->
                embedCreator.apply(payload)
                .flatMap(embed -> 
                    sendMessageToServer(serverClan.server().serverId(), embed)
                )
            )
            .then()
            .doOnSuccess(v -> System.out.println("All alerts sent successfully"))
            .doOnError(e -> System.err.println("Error sending one or more alerts: " + e.getMessage()));
    }

    /**
     * Helper method to send a message to a specific server.
     */
    private Mono<Void> sendMessageToServer(String serverId, EmbedCreateSpec embed) {
        return client.getGuildById(Snowflake.of(serverId))
            .flatMap(guild -> 
                clashBotApiClient.getServerById(serverId).flatMap(server ->
                    Mono.justOrEmpty(server.defaultTextChannel())
                ).flatMap(channelId -> 
                    guild.getChannelById(Snowflake.of(channelId))
                ).switchIfEmpty(
                    guild.getSystemChannel().doOnError(e -> System.err.println("Error getting system channel for guild: " + e.getMessage()))
                )
            )
            .cast(MessageChannel.class)
            .doOnError(e -> System.err.println("Error casting retrieved channel to TextChannel: " + e.getMessage()))
            .flatMap(channel -> channel.createMessage(embed))
            .doOnSuccess(msg -> System.out.println("Message sent to server: " + serverId))
            .doOnError(e -> System.err.println("Failed to send message to server: " + serverId + " - " + e.getMessage()))
            .then()
            .onErrorResume(e -> Mono.empty()); // Suppress error to continue sending other alerts
    }

    /**
     * Specific embed creation methods.
     */

    public <T extends Alert> Mono<EmbedCreateSpec> createEmbed(AlertEmbeddingBuilder<T> payload) {
        return payload.materialize();
    }
}
