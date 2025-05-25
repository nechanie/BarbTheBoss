package com.clashbot.discordbot.events;

import com.clashbot.database.ClashBotApiClient;

import discord4j.core.event.domain.guild.GuildDeleteEvent;
import reactor.core.publisher.Mono;

public class GuildDeleteListener implements EventListener<GuildDeleteEvent> {

    private final ClashBotApiClient apiClient;

    public GuildDeleteListener(ClashBotApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Class<GuildDeleteEvent> getEventType() {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<Void> onEvent(GuildDeleteEvent event) {
        if (!event.isUnavailable()) {
            String guildId = event.getGuildId().asString();
            return apiClient.deleteServer(guildId).then();
        }
        return Mono.empty();
    }
}
