package com.clashbot.discordbot.embeds.System;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image.Format;
import reactor.core.publisher.Mono;

public class ServerJoinEmbed implements SystemEmbed<ServerJoinEmbed, GuildCreateEvent> {
    
    private Builder embed = EmbedCreateSpec.builder();
    private String plainText;

    public ServerJoinEmbed setViewContent(GuildCreateEvent event){
        Guild guild = event.getGuild();
        String guildName = guild.getName();
        String guildIconUrl = guild.getIconUrl(Format.JPEG).orElse(null);

        embed.author(guildName + " Setup Guide", null, guildIconUrl)
            .title("🚀 Getting Started")
            .addField("1️⃣ Register your clan 🔒",
                "`/register clan <clan_tag>`\n" +
                "Register your Clash of Clans clan so I know which clan to track for **" + guildName + "**.",
                false)
            .addField("2️⃣ Let members link accounts",
                "`/register account <account_tag>`\n" +
                "Any member can link one or more COC player accounts to their Discord user.",
                false)
            .addField("3️⃣ Set the default clan 🔒",
                "`/default clan view`\n" +
                "Check your default. If none is set, run:\n" +
                "`/default clan edit <clan_tag>`",
                false)
            .addField("4️⃣ Configure clan settings 🔒",
                "`/settings view clan`\n" +
                "See current defaults (all OFF by default).\n" +
                "To change them, use:\n" +
                "`/settings edit clan` → select options from the menu.",
                false)
            .addField("5️⃣ Change default message channel 🔒",
                "By default I post to your system messages channel. To change:\n" +
                "`/default channel edit <#channel>`",
                false)
            .addField("🔒 Legend",
                "🔒 Requires **Manage Server** permission\n" +
                "— only moderators can run those steps",
                false)
            .color(Color.of(114, 137, 218));

        return this;
    }

    public ServerJoinEmbed setAdditionalContent(GuildCreateEvent event){

        embed.title("⚙️ Core Clan Settings Explained")
        .color(Color.of(114, 137, 218))
        .description(
            "• **Send war attack reminders** — 2 hours before war end, I’ll ping who still has attacks.\n" +
            "• **Send war signup reminders** — After 12 hours without a war, I’ll remind leadership.\n" +
            "• **Send War News Updates** — Posts key war events (e.g. lead change, 3★ attacks, perfect war) when enabled."
        )
        .footer("Need more help? Try `/help` or ask a moderator to check the wiki.", null);

        return this;

    }

    public ServerJoinEmbed setPlainTextContent(GuildCreateEvent event){
        Guild guild = event.getGuild();
        String guildName = guild.getName();

        plainText = String.format(
            "**👋 Welcome to Barb The Boss!**\n\n" +
            "Hello **%s** moderators! I’m Barb The Boss, here to help you manage your Clash of Clans clan right from Discord. \n\n" +
            "Below is a quick-start guide—A majority of the initial setup will require a server moderator with **Manage Server** permission. These permission requirements are marked with 🔒.\n" +
            "_Don’t worry: regular members can still link their own Clash accounts!_\n",
            guildName
        );

        return this;
    }

    public String materializePlain(){
        return plainText;
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono(){
        return Mono.just(embed.build());
    }

    @Override
    public EmbedCreateSpec materialize(){
        return embed.build();
    }

}
