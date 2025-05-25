package com.clashbot.discordbot.commands;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.RegistrationsEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class RegistrationsCommand implements SlashCommand {

    @Override
    public String getName() {
        return "registrations";
    }

    private final ClashBotApiClient api;

    public RegistrationsCommand(ClashBotApiClient api) {
        this.api = api;
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(RegistrationsEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
        .name(getName())
        .description("View registered clans or accounts.")
        .addOption(ApplicationCommandOptionData.builder()
            .name("clans")
            .description("View clans registered in this discord server.")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .build()
        )
        .addOption(ApplicationCommandOptionData.builder()
            .name("accounts")
            .description("View account registered to your discord account.")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                .name("user")
                .description("Mention a Discord user to view their account registrations.")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(false)
                .build()
            )
            .build()
        )
        .build();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String subCommandName = event.getOptions().get(0).getName(); // "clans" or "accounts"

        return switch (subCommandName) {
            case "clans" -> handleClans(event);

            case "accounts" -> {
                try {
                    Mono<User> user = event.getOptions().get(0).getOption("user")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .map(ApplicationCommandInteractionOptionValue::asUser)
                        .get();

                    yield user.flatMap(nonNullUser -> handleAccounts(event, nonNullUser));
                }
                catch (NoSuchElementException e){
                    yield handleAccounts(event, null);
                }
            }

            default -> event.reply("❌ Unknown type `" + subCommandName + "`. Use clans or accounts.")
                        .withEphemeral(true)
                        .then();
        };
    }

    public Mono<Void> handleClans(ChatInputInteractionEvent event){
        String serverId;
        try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }

        return event.deferReply().withEphemeral(false)
            .thenMany(api.listAllServerClanMapsByDiscord(serverId))
            .collectList()
            .flatMap(maps ->
                event.getInteraction().getGuild().flatMap(guild -> 
                    event.editReply()
                    .withEmbeds(
                        SlashCommandEmbed.init(RegistrationsEmbed.class)
                            .setViewContentClans(maps, guild.getName())
                            .materialize()
                    )
                )
            )
            .onErrorResume(e ->
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            ).then();
    }

    public Mono<Void> handleAccounts(ChatInputInteractionEvent event, User user){

        String queryUserId = user != null ? user.getId().asString() : event.getInteraction().getUser().getId().asString();

        
        return event.deferReply().withEphemeral(false)
                .thenMany(api.getAllMappingsForDiscord(queryUserId))
                .collectList()
                .flatMap(maps ->
                    event.editReply()
                         .withEmbeds(
                             SlashCommandEmbed.init(RegistrationsEmbed.class)
                                 .setViewContentAccounts(maps, user != null ? user.getUsername() : event.getInteraction().getUser().getUsername())
                                 .materialize()
                         )
                )
            .onErrorResume(e ->
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            ).then();
    }
}
