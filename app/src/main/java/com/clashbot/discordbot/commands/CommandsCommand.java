package com.clashbot.discordbot.commands;

import java.util.List;

import com.clashbot.discordbot.embeds.CommandEmbeds.CommandsEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class CommandsCommand implements SlashCommand {

    private final List<SlashCommand> allCommands;

    public CommandsCommand(List<SlashCommand> allCommands) {
        this.allCommands = allCommands;
    }



    @Override
    public String getName() {
        return "commands";
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
            .name(getName())
            .description("List all available bot commands")
            .build();
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(CommandsEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply()
            .withEphemeral(true)
            .withEmbeds(
                SlashCommandEmbed.init(CommandsEmbed.class)
                    .setViewContent(allCommands)
                    .materialize()
            )
            .then();
    }
}
