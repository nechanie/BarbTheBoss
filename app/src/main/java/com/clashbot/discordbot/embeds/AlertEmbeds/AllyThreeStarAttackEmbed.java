package com.clashbot.discordbot.embeds.AlertEmbeds;

import java.util.List;

import com.clashbot.models.AllyThreeStarAttack;

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
public class AllyThreeStarAttackEmbed extends AllyThreeStarAttack implements AlertEmbeddingBuilder<AllyThreeStarAttack> {

    private Builder embed = EmbedCreateSpec.builder();

    @Override
    public Mono<EmbedCreateSpec> materialize() {
        String message =
        """
        ## ⭐⭐⭐ Ally Three Star Attack ⭐⭐⭐

        One or more allies just performed a perfect attack in the clan war!

        ### Allies who performed a 3 ⭐ attack:
        %s
        """;
        return prepareMessage(this.getAttacks(), message)
            .flatMap(fullMessage -> {
                embed.author(this.getServerClans().getFirst().clan().clanName() + " - War Event", null, null)
                    .description(fullMessage)
                    .footer("This is an automated message. Clan: " + this.getServerClans().getFirst().clan().clanId(), null);
                return Mono.empty();
            })
            .then(Mono.defer(() -> Mono.fromCallable(() -> embed.build())));
    }

    private Mono<String> prepareMessage(List<AttackInfo> attacks, String contextString) {
        return Flux.fromIterable(attacks)
            .collect(
                () -> new StringBuilder(),
                (builder, content) -> builder.append("- ").append(content.attackerName()).append("\n")
            )
            .flatMap(builder -> {
                if (builder.length() == 0) {
                    builder.append("No three-star attacks performed.");
                }
                
                return Mono.just(String.format(contextString, builder.toString()));
            });
    }
}
