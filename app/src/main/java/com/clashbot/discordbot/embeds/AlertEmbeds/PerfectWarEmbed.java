package com.clashbot.discordbot.embeds.AlertEmbeds;

import com.clashbot.models.PerfectWar;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class PerfectWarEmbed extends PerfectWar implements AlertEmbeddingBuilder<PerfectWar> {
    private Builder embed = EmbedCreateSpec.builder();

    @Override
    public Mono<EmbedCreateSpec> materialize(){
        embed.author(this.getServerClans().getFirst().clan().clanName() + " - War Event", null, null)
        .title("🏅 Perfect War")
        .description(this.getClanName() + " has achieved a perfect war against " + this.getOpponentName() + "!\n　\n　")
        .footer("This is an automated message. Clan: " + this.getServerClans().getFirst().clan().clanId(), null);
        return Mono.fromCallable(() -> embed.build());
    }
}
