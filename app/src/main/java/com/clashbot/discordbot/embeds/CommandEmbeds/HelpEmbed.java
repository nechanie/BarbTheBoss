package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.util.ResourceBundle;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class HelpEmbed implements SlashCommandEmbed<HelpEmbed> {

    private Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    @Override
    public EmbedCreateSpec materialize(){
        return embed.build();
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono(){
        return Mono.just(embed.build());
    }

    public HelpEmbed setViewContent(){
        return this;
    }

    @Override
    public HelpEmbed setHelp() {
        embed.author(resources.getString("help.help.author"), null, null)
            .title(resources.getString("help.help.title"))
            .description(resources.getString("help.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("help.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("help.help.examples.value"), false)
            .addField(resources.getString("shared.help.tip.name"), resources.getString("help.help.tip.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("help.help.footer"), null);
        return this;
    }
}
