package com.clashbot.discordbot.embeds.AlertEmbeds;

import java.util.List;

import com.clashbot.models.AttackReminder;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class AttackReminderEmbed extends AttackReminder implements AlertEmbeddingBuilder<AttackReminder> {

    private final Builder embed = EmbedCreateSpec.builder();

    @Override
    public Mono<EmbedCreateSpec> materialize() {
        embed.author(this.getServerClans().getFirst().clan().clanName() + " - Clan Reminder", null, null)
            .footer("This is an automated message. Clan: " + this.getServerClans().getFirst().clan().clanId(), null);
    
        return Flux.fromIterable(this.getMembers())
            .flatMap(memberInfo -> {
                if (memberInfo.discordAccount() != null) {
                    return Mono.just(memberInfo.member().getName() + " (<@" + memberInfo.discordAccount().discordId() + ">)" 
                        + " - " + (this.getAttacksPerMember() - memberInfo.member().getAttacks().size()) + " left");
                } else {
                    return Mono.just(memberInfo.member().getName() + " - " 
                        + (this.getAttacksPerMember() - memberInfo.member().getAttacks().size()) + " left");
                }
            })
            .collectList()
            .flatMap(collection -> prepareFields(collection)
                .flatMap(playersString -> 
                    Mono.fromCallable(() -> {
                        String description = String.format("""
                                # ⚔️ Attack Reminder - %s

                                %s

                                %s
                                \u200B
                                """,
                            this.getServerClans().getFirst().clan().clanName(),
                            collection.isEmpty() ? "Everyone has used their attacks! Good job everyone!"
                            : "There are 2 hours left in the war. It is time to attack!",
                            playersString);
                    embed.description(description);
                    return embed.build();
                }))
            );
    }

    private Mono<String> prepareFields(List<String> collection) {
        return Flux.fromIterable(collection)
        .collect(StringBuilder::new, (builder, content) -> builder.append("- ").append(content).append("\n"))
        .flatMap(builder -> {
            if (builder.length() == 0) {
                builder.append("No members with attacks left!");
            }
            return Mono.just("## __Members with attacks left__\n\u200B\n" + builder.toString());
        });
    }
}
