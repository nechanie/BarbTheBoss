package com.clashbot.discordbot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import com.clashbot.discordbot.embeds.CommandEmbeds.HelpEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;

public class HelpCommand implements SlashCommand {

    private final List<SlashCommand> allCommands;

    public HelpCommand(List<SlashCommand> allCommands) {
        this.allCommands = allCommands;
    }
   
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(HelpEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
            .name(getName())
            .description("Show help for commands")
            .addOption(ApplicationCommandOptionData.builder()
                .name("command_choice")
                .description("The command you want help with (e.g. register, unregister, show)")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .autocomplete(true)
                .required(false)
                .build())
            .build();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<String> cmdOpt = event.getOption("command_choice")
            .flatMap(opt -> opt.getValue().map(v -> v.asString()));

        return cmdOpt
            .map(cmdName -> Flux.fromIterable(allCommands)
                .filter(cmd -> cmd.getName().equalsIgnoreCase(cmdName))
                .single()
                .flatMap(cmd -> cmd.help(event))
                .onErrorResume(e -> help(event)))
            .orElseGet(() -> help(event));
    }
}