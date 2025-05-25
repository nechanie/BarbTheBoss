package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.lycoon.clashapi.models.war.War;
import com.lycoon.clashapi.models.war.enums.WarState;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;


public class ShowEmbed implements SlashCommandEmbed<ShowEmbed> {
    private final Builder embed = EmbedCreateSpec.builder();
    private ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    @Override
    public Mono<EmbedCreateSpec> materializeMono(){
        return Mono.just(embed.build());
    }

    @Override
    public EmbedCreateSpec materialize(){
        return embed.build();
    }

    @Override
    public ShowEmbed setHelp() {
        embed.author(resources.getString("show.help.author"), null, null)
            .title(resources.getString("show.help.title"))
            .description(resources.getString("show.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("show.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("show.help.examples.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218))
            .footer(resources.getString("show.help.footer"), null);
        return this;
    }

    public Mono<ShowEmbed> setViewContent(War currentWar) {

        return Mono.zip(
            formatInstant(currentWar.getStartTime()),
            formatInstant(currentWar.getEndTime()),
            Mono.fromCallable(() -> decodeEnum(currentWar.getState())),
            isClanLeading(currentWar)
        ).flatMap(tuple -> {
            String startTime = tuple.getT1();
            String endTime = tuple.getT2();
            String state = tuple.getT3();
            Boolean isClanLeading = tuple.getT4();
            int totalStars = currentWar.getTeamSize() * 3;
            return Mono.fromCallable(() -> {
                embed.author(resources.getString("show.war.author").formatted(currentWar.getClan().getName()), null, null)
                    .thumbnail(currentWar.getClan().getBadgeUrls().getMedium())
                    .description(resources.getString("show.war.description").formatted(
                            currentWar.getClan().getName(),
                            currentWar.getTeamSize(),
                            currentWar.getTeamSize(),
                            currentWar.getAttacksPerMember(),
                            state,
                            startTime,
                            endTime
                        )
                    )
                    .addField(resources.getString("show.war.ally.name").formatted(currentWar.getClan().getName()),
                        resources.getString("show.war.ally.value").formatted(
                            currentWar.getClan().getAttacks(),
                            currentWar.getAttacksPerMember() * currentWar.getTeamSize(),
                            currentWar.getClan().getStars(),
                            totalStars,
                            currentWar.getClan().getDestructionPercentage()
                        ),
                        true)
                    .addField(resources.getString("show.war.opponent.name").formatted(currentWar.getOpponent().getName()),
                        resources.getString("show.war.opponent.value").formatted(
                            currentWar.getOpponent().getAttacks(),
                            (currentWar.getAttacksPerMember() * currentWar.getTeamSize()),
                            currentWar.getOpponent().getStars(),
                            totalStars,
                            currentWar.getOpponent().getDestructionPercentage()
                        ),
                        true)
                    .addField(isClanLeading ? resources.getString("show.war.status.name.ally") : resources.getString("show.war.status.name.opponent"),
                        resources.getString("show.war.status.value"),
                        false)
                    .footer(resources.getString("show.war.footer").formatted(currentWar.getClan().getTag()), null);
                return embed;
            }).thenReturn(this);
        });
    }

    private Mono<Boolean> isClanLeading(War war){
        return Mono.zip(
            Mono.just(war.getClan().getStars()), 
            Mono.just(war.getOpponent().getStars()),
            Mono.just(war.getOpponent().getDestructionPercentage()),
            Mono.just(war.getClan().getDestructionPercentage())
        )
        .map(tuple -> {
            int clanStars = tuple.getT1();
            int opponentStars = tuple.getT2();
            double opponentDestruction = tuple.getT3();
            double clanDestruction = tuple.getT4();

            if (clanStars != opponentStars) {
                return clanStars > opponentStars;
            } else {
                return opponentDestruction > clanDestruction;
            }
        });
    }

    private Mono<String> formatInstant(String timestamp) {
        return Mono.fromCallable(() -> {
            try {
                // Define the date pattern to match the API format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX")
                        .withZone(ZoneId.of("UTC"));

                // Parse the string to an Instant
                Instant instant = Instant.from(formatter.parse(timestamp));

                // Convert to PST time zone
                ZonedDateTime pstTime = instant.atZone(ZoneId.of("America/Los_Angeles"));

                // Format the date and time for printing
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

                return pstTime.format(outputFormatter);
            } catch (Exception e) {
                // Handle potential parsing errors
                return "Invalid Date";
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private String decodeEnum(WarState state) {
        if (state == null) {
            return "Unknown State";
        }

        switch (state) {
            case ENDED:
                return "War Ended";
            case PREPARATION:
                return "Preparation";
            case IN_MATCHMAKING:
                return "Matchmaking";
            case IN_WAR:
                return "In War";
            default:
                return "Unknown State";
        }
    }

}
