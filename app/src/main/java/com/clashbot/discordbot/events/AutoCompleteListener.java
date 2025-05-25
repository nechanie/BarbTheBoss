package com.clashbot.discordbot.events;

import java.util.List;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.commands.SlashCommand;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import reactor.core.publisher.Mono;

public class AutoCompleteListener implements EventListener<ChatInputAutoCompleteEvent> {
    
    private final ClashBotApiClient api;
    private final List<ApplicationCommandOptionChoiceData> commandChoices;

    public AutoCompleteListener(ClashBotApiClient api, List<SlashCommand> allCommands) {
        this.api = api;
        // build once, reuse forever
        this.commandChoices = allCommands.stream()
            .map(cmd -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
                             .name(cmd.getName())
                             .value(cmd.getName())
                             .build())
            .toList();
    }

    @Override
    public Class<ChatInputAutoCompleteEvent> getEventType() {
        return ChatInputAutoCompleteEvent.class;
    }

   @Override
public Mono<Void> onEvent(ChatInputAutoCompleteEvent event) {
    String focused = event.getFocusedOption().getName();
    // the partial text the user has typed so far
    String input = event.getFocusedOption()
                        .getValue()
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .map(String::toLowerCase)
                        .orElse("");

    Mono<List<ApplicationCommandOptionChoiceData>> suggestions;
    switch (focused) {
        case "clan_tag" -> {
            String serverId;
            try {
                serverId = event.getInteraction().getGuildId()
                    .map(Snowflake::asString)
                    .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
                suggestions = api.listAllServerClanMapsByDiscord(serverId)
                .filter(map -> map.clan().clanName()
                                .toLowerCase()
                                .startsWith(input))
                .map(map -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
                                .name(map.clan().clanName())
                                .value(map.clan().clanId())
                                .build())
                .take(25)
                .collectList();
            } catch (Exception e) {
                suggestions = Mono.just(List.of());
            }
        }
        case "account_tag" -> {
            suggestions = api.getAllMappingsForDiscord(event.getInteraction()
                                                            .getUser()
                                                            .getId()
                                                            .asString())
                .filter(map -> map.clashAccount().username()
                                .toLowerCase()
                                .startsWith(input))
                .map(map -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
                                .name(map.clashAccount().username())
                                .value(map.clashAccount().clashId())
                                .build())
                .take(25)
                .collectList();
        }
        case "channel_choice" -> {
            try {
                suggestions = event.getInteraction()
                .getGuild()
                .flatMapMany(g -> g.getChannels())
                .filter(ch -> ch.getType() == Channel.Type.GUILD_TEXT)
                .filter(ch -> ch.getName().toLowerCase().startsWith(input))
                .map(ch -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
                                .name(ch.getName())
                                .value(ch.getId().asString())
                                .build())
                .take(25)
                .collectList();
            } catch (Exception e) {
                suggestions = Mono.just(List.of());
            }
        }
        case "command_choice" -> {
            // just filter your pre-built list
            List<ApplicationCommandOptionChoiceData> filtered = commandChoices.stream()
                .filter(c -> c.name().toLowerCase().startsWith(input))
                .limit(25)
                .toList();
            suggestions = Mono.just(filtered);
        }
        default -> suggestions = Mono.just(List.of());
    }
    return suggestions.flatMap(event::respondWithSuggestions);
    }
}
