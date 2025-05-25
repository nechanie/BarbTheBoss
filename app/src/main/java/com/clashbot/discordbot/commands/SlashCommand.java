package com.clashbot.discordbot.commands;

import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface SlashCommand {
    String getName();
    ApplicationCommandRequest getCommandRequest();
    Mono<Void> handle(ChatInputInteractionEvent event);

    Mono<Void> help(ChatInputInteractionEvent event);
    
    default String getDescription() {
        return getCommandRequest().description().toOptional().orElse("No description provided.");
    }
}