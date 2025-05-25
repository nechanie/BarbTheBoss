package com.clashbot.discordbot.embeds.AlertEmbeds;

import com.clashbot.models.LeadChange;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class LeadChangeEmbed extends LeadChange implements AlertEmbeddingBuilder<LeadChange> {

    private Builder embed = EmbedCreateSpec.builder();

    @Override
    public Mono<EmbedCreateSpec> materialize(){
        embed.author(this.getServerClans().getFirst().clan().clanName() + " - War Event", null, null)
        .description(
            """
            ## ⚠️ Clan War Lead Change

            The leading clan in the war has changed!
            """
        )
        .image(this.isClanLeading() ? "https://i.imgur.com/u0dMBjE.png" : "https://i.imgur.com/M2rPeE1.png")
        .footer("This is an automated message. Clan: " + this.getServerClans().getFirst().clan().clanId(), null);

        return prepareFields(this.getClanName(), this.getOpponentName(), this.isClanLeading()).then(Mono.fromCallable(() -> embed.build()));
    }

    private Mono<Void> prepareFields(String clanName, String oppName, Boolean isClanLead) {
        return Mono.fromRunnable(() ->{
                embed.addField("__Ally Clan__", "\n\n" + clanName, true);
                embed.addField("__Opponent Clan__", "\n\n" + oppName, true);
                embed.addField("__Status__", String.format("\u200B\n%s\u200B", isClanLead ? "**🏆🏆Ally clan is in the lead!🏆🏆**" : " **‼️Enemy clan is in the lead!‼️**"), false);
            });
    }
}
