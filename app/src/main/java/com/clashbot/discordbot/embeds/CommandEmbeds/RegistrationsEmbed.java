package com.clashbot.discordbot.embeds.CommandEmbeds;

import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.ServerClanMapDto;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

public class RegistrationsEmbed implements SlashCommandEmbed<RegistrationsEmbed> {

    private Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    @Override
    public RegistrationsEmbed setHelp() {
        embed.author(resources.getString("registrations.help.author"), null, null)
            .title(resources.getString("registrations.help.title"))
            .description(resources.getString("registrations.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("registrations.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("registrations.help.examples.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("registrations.help.footer"), null);
        return this;
    }

    @Override
    public EmbedCreateSpec materialize() {
        return embed.build();
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono() {
        return Mono.just(embed.build());
    }

    /**
     * List account mappings for a Discord user.
     */
    public RegistrationsEmbed setViewContentAccounts(List<AccountMapDto> maps, String discordUserName) {
        embed.title(resources.getString("registrations.accounts.title").formatted(discordUserName))
            .description(resources.getString("registrations.accounts.description"))
            .timestamp(Instant.now())
            .color(Color.of(76, 175, 80)); // green

        if (maps.isEmpty()) {
            embed.addField(
                resources.getString("registrations.accounts.empty.name"),
                resources.getString("registrations.accounts.empty.value"),
                false
            );
        } else {
            maps.forEach(account -> 
                embed.addField(
                    resources
                        .getString("registrations.accounts.item.name")
                        .formatted(
                            account.clashAccount().username(),
                            account.clashAccount().clashId()
                        ),
                    resources
                        .getString("registrations.accounts.item.value")
                        .formatted(
                            account.clashAccount().clan().clanName(),
                            account.clashAccount().isLeader() ? "Yes" : "No"
                        ),
                    false
                )
            );
        }
        return this;
    }

    /**
     * List clan mappings for this Discord server.
     */
    public RegistrationsEmbed setViewContentClans(List<ServerClanMapDto> maps, String serverName) {
        embed.title(resources.getString("registrations.clans.title").formatted(serverName))
            .description(resources.getString("registrations.clans.description"))
            .timestamp(Instant.now())
            .color(Color.of(33, 150, 243)); // blue

        if (maps.isEmpty()) {
            embed.addField(
                resources.getString("registrations.clans.empty.name"),
                resources.getString("registrations.clans.empty.value"),
                false
            );
        } else {
            maps.forEach(map -> {
                embed.addField(
                    resources.getString("registrations.clans.item.name")
                        .formatted(map.clan().clanName(), map.clan().clanId()),
                    resources.getString("registrations.clans.item.value")
                        .formatted(map.defaultServerClan() ? "Yes" : "No"),
                    false
                );
            });
        }
        return this;
    }
}