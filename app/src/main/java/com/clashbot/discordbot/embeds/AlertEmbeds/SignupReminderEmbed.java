package com.clashbot.discordbot.embeds.AlertEmbeds;


import com.clashbot.models.SignupReminder;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class SignupReminderEmbed extends SignupReminder implements AlertEmbeddingBuilder<SignupReminder> {
    private Builder embed = EmbedCreateSpec.builder();

    @Override
    public Mono<EmbedCreateSpec> materialize(){
        embed.author(this.getServerClans().getFirst().clan().clanName() + " - Clan Reminder", null, null)
        .title("War Signup Reminder")
        .description("Attention " + this.getClanName() + " clan leaders. It has been over 12 hours since the last clan war ended! Don't forget to start a new clan war search!")
        .footer("This is an automated message. Clan: " + this.getServerClans().getFirst().clan().clanId(), null);
        return Mono.fromCallable(() ->embed.build());
    }
}
