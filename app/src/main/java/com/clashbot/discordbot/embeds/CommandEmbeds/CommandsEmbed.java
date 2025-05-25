package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.util.List;
import java.util.ResourceBundle;

import com.clashbot.discordbot.commands.SlashCommand;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class CommandsEmbed implements SlashCommandEmbed<CommandsEmbed> {
    private Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    /**
     * Populate the embed with the provided commands list.
     */
    public CommandsEmbed setViewContent(List<SlashCommand> commands) {
        embed.title(resources.getString("commands.title"))
             .description(resources.getString("commands.dedscription"))
             .color(Color.of(114, 137, 218));

        commands.stream()
            .filter(cmd -> !cmd.getName().equalsIgnoreCase("commands"))  // omit itself
            .forEach(cmd -> embed.addField(
                    "/" + cmd.getName(),
                    cmd.getDescription(),
                    false
                )
            );
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

    @Override
    public CommandsEmbed setHelp() {
        embed.author(resources.getString("commands.help.author"), null, null)
            .title(resources.getString("commands.help.title"))
            .description(resources.getString("commands.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("commands.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("commands.help.examples.value"), false)
            .addField(resources.getString("shared.help.tip.name"), resources.getString("commands.help.tip.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("commands.help.footer"), null);
        return this;
    }
}
