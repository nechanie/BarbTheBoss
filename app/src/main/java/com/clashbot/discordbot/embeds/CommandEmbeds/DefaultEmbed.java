package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.util.ResourceBundle;

import com.clashbotbackend.dto.ClanDto;

import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class DefaultEmbed implements SlashCommandEmbed<DefaultEmbed>{
    
    private Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    public DefaultEmbed setViewContent(String serverName, ClanDto defaultClan){
        embed.author(resources.getString("default.author").formatted(serverName), null, null)
            .description(
                resources.getString("default.description")
                    .formatted(
                        "clan",
                        serverName,
                        "clan",
                        serverName,
                        defaultClan.clanName() + "(" + defaultClan.clanId() + ")"
                    )
            );
        return this;
    }

    public DefaultEmbed setViewContent(String serverName, TextChannel defaultChannel){
        embed.author(resources.getString("default.author").formatted(serverName), null, null)
            .description(
                resources.getString("default.description")
                    .formatted(
                        "text channel",
                        serverName,
                        "text channel",
                        serverName,
                        defaultChannel.getName()
                    )
            );
        return this;
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono(){
        return Mono.just(embed.build());
    }

    @Override
    public EmbedCreateSpec materialize(){
        return embed.build();
    }

    @Override
    public DefaultEmbed setHelp() {
        embed.author(resources.getString("default.help.author"), null, null)
            .title(resources.getString("default.help.title"))
            .description(resources.getString("default.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("default.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("default.help.examples.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("default.help.footer"), null);
        return this;
    }


}
