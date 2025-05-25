package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.time.Instant;
import java.util.ResourceBundle;

import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.ServerClanMapDto;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;


public class RegisterEmbed implements SlashCommandEmbed<RegisterEmbed> {
    
    private static ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");
    private Builder embed = EmbedCreateSpec.builder();

    @Override
    public EmbedCreateSpec materialize() {
        return embed.build();
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono() {
        return Mono.just(embed.build());
    }

    /**
     * Display the result of a clan registration lookup or creation.
     */
    public RegisterEmbed setViewContent(ServerClanMapDto map) {
        embed.title(resources.getString("register.clan.title"))
            .description(
                resources.getString("register.clan.description")
                    .formatted(map.clan().clanName(), map.clan().clanId())
            )
            .color(Color.of(114, 137, 218))
            .timestamp(Instant.now());
        return this;
    }
    public RegisterEmbed setExistingViewContent(ServerClanMapDto map) {
        embed.title(resources.getString("register.clan.existing.title"))
            .description(
                resources.getString("register.clan.existing.description")
                    .formatted(map.clan().clanName(), map.clan().clanId())
            )
            .color(Color.of(114, 137, 218))
            .timestamp(Instant.now());
        return this;
    }

    public static InteractionPresentModalSpec getVerificationModal(){
        return InteractionPresentModalSpec.builder()
            .title(resources.getString("register.account.verification.modal.title"))
            .customId("account-verification-modal")
            .addComponent(
                ActionRow.of(
                    TextInput.small(
                        "account-verification-token",
                        resources.getString("register.account.verification.modal.input.label"),
                        resources.getString("register.account.verification.modal.input.placeholder")
                    )
                    .required(true)
                )
            ).build();
    }

    public RegisterEmbed setExistingViewContent(AccountMapDto map) {
        embed.title(resources.getString("register.account.exitsting.title"))
            .description(resources.getString("register.account.existing.description")
                .formatted(map.clashAccount().username(), map.clashAccount().clashId())
            )
            .color(Color.of(114, 137, 218))
            .timestamp(Instant.now());
        return this;
    }

    /**
     * Display the result of an account registration lookup or creation.
     */
    public RegisterEmbed setViewContent(AccountMapDto map) {
        embed.title(resources.getString("register.account.title"))
            .description(
                resources.getString("register.account.description").formatted(map.clashAccount().username(), map.clashAccount().clashId())
            )
            .color(Color.of(114, 137, 218))
            .timestamp(Instant.now());
        return this;
    }

    @Override
    public RegisterEmbed setHelp() {
        embed.author(resources.getString("register.help.author"), null, null)
            .title(resources.getString("register.help.title"))
            .description(resources.getString("register.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("register.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("register.help.examples.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("register.help.footer"), null);
        return this;
    }
}
