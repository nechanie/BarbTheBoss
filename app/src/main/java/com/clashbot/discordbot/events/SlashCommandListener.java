package com.clashbot.discordbot.events;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.clashbot.discordbot.commands.SlashCommand;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public class SlashCommandListener implements EventListener<ChatInputInteractionEvent> {

    private final Map<String, SlashCommand> commandMap;

    public SlashCommandListener(List<SlashCommand> commands) {
        this.commandMap = commands.stream().collect(Collectors.toMap(SlashCommand::getName, cmd -> cmd));
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<Void> onEvent(ChatInputInteractionEvent event) {
        SlashCommand command = commandMap.get(event.getCommandName());
        if (command != null) {
            return command.handle(event);
        }
        return Mono.empty();
    }
}