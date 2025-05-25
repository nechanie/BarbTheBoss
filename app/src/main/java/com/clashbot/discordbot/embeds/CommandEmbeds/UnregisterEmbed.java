package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.time.Instant;
import java.util.ResourceBundle;

import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.ServerClanMapDto;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class UnregisterEmbed implements SlashCommandEmbed<UnregisterEmbed> {
    
    private Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    @Override
    public EmbedCreateSpec materialize() {
        return embed.build();
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono() {
        return Mono.just(embed.build());
    }

    /**
     * Show confirmation for a clan unregistration.
     */
    public UnregisterEmbed setViewContent(ServerClanMapDto map) {
        embed.title(resources.getString("unregister.clan.title"))
             .description(resources.getString("unregister.clan.description").formatted(
                 map.clan().clanName(), map.clan().clanId()
             ))
             .color(Color.of(114, 137, 218))
             .timestamp(Instant.now());
        return this;
    }

    /**
     * Show confirmation for an account unregistration.
     */
    public UnregisterEmbed setViewContent(AccountMapDto map) {
        embed.title(resources.getString("unregister.account.title"))
             .description(resources.getString("unregister.account.description").formatted(
                 map.clashAccount().username(), map.clashAccount().clashId()
             ))
             .color(Color.of(114, 137, 218))
             .timestamp(Instant.now());
        return this;
    }

    @Override
    public UnregisterEmbed setHelp() {
        embed.author(resources.getString("unregister.help.author"), null, null)
            .title(resources.getString("unregister.help.title"))
            .description(resources.getString("unregister.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("unregister.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("unregister.help.examples.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("unregister.help.footer"), null);
        return this;
    }
}
