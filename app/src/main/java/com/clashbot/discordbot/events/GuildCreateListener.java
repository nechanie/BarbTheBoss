package com.clashbot.discordbot.events;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.System.ServerJoinEmbed;
import com.clashbot.discordbot.embeds.System.SystemEmbed;
import com.clashbotbackend.dto.CreateServerRequest;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;

public class GuildCreateListener implements EventListener<GuildCreateEvent> {

    private final ClashBotApiClient apiClient;

    public GuildCreateListener(ClashBotApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Class<GuildCreateEvent> getEventType() {
        return GuildCreateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(GuildCreateEvent event) {
        Guild guild = event.getGuild();
        String guildId = guild.getId().asString();

        CreateServerRequest req = new CreateServerRequest();
        req.setServerId(guildId);

        return apiClient.getServerById(guildId)
            // if not found, create and then send greeting, but still return the Server
            .switchIfEmpty(
                apiClient.createServer(req)
                .flatMap(created ->
                    guild.getSystemChannel()
                        .flatMap(ch ->
                            ch.createMessage(
                                SystemEmbed.init(ServerJoinEmbed.class).setPlainTextContent(event).materializePlain()
                            ).withEmbeds(
                                SystemEmbed.init(ServerJoinEmbed.class).setViewContent(event).materialize(),
                                SystemEmbed.init(ServerJoinEmbed.class).setAdditionalContent(event).materialize()
                            )
                        )
                        // drop the Message and return the original Server
                        .thenReturn(created)
                )
            )
            // at this point we have a Mono<Server> whether it existed or was just created
            // we don't need the Server value, so drop it and return Mono<Void>
            .then();
    }
}
