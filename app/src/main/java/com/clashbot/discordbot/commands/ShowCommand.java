package com.clashbot.discordbot.commands;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.ShowEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;
import com.clashbotbackend.dto.ClanDto;
import com.lycoon.clashapi.core.ClashAPI;
import com.lycoon.clashapi.core.exceptions.ClashAPIException;
import com.lycoon.clashapi.models.war.War;
import com.lycoon.clashapi.models.war.enums.WarState;
import com.lycoon.clashapi.models.warleague.WarLeagueGroup;
import com.lycoon.clashapi.models.warleague.WarLeagueRound;
import com.lycoon.clashapi.models.warleague.enums.WarLeagueGroupState;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class ShowCommand implements SlashCommand {
    
    
    private static final Logger log = LoggerFactory.getLogger(ShowCommand.class);

    private final ClashBotApiClient api;
    private final ClashAPI clashAPI;

    public ShowCommand(ClashBotApiClient api, ClashAPI clashAPI) {
        this.api = api;
        this.clashAPI = clashAPI;
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(ShowEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
        .name("show")
        .description("Quick acces to various information.")
        .addOption(ApplicationCommandOptionData.builder()
            .name("war")
            .description("View live war information.")
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("clan_tag")
                    .description("The clan tag to view current war info of.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .autocomplete(true)
                    .build()
            )
            .build()
        )
        .build();

    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String subCommandName = event.getOptions().get(0).getName();

        String tag = event.getOption(subCommandName)
            .flatMap(opt -> opt.getOption("clan_tag"))
            .flatMap(opt -> opt.getValue().map(v -> v.asString()))
            .orElse("");
        
        return switch (subCommandName) {
            case "war" -> handleWar(tag, event);
            default -> event.reply("❌ Unknown subcommand `" + subCommandName + "`. Use clan or account.")
                        .withEphemeral(true)
                        .then();
        };
      
    }

    private Mono<Void> handleWar(String tag, ChatInputInteractionEvent event) {
        Mono<String> clanTag;
        if (tag.isEmpty()) {
            String serverId;
            try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
            } catch (Exception e) {
                return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
            }
            clanTag = api.getDefaultServerClanMap(serverId)
                .flatMap(map -> Mono.just(map.clan().clanId()))
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "❌ No default clan registered for this server."
                )));
        } else {
            clanTag = Mono.just(tag);
        }
    
        // Defer reply immediately to prevent timeout
        return event.deferReply().withEphemeral(true)
            .then(clanTag.flatMap(id -> api.getClanById(id)
                    .switchIfEmpty(event.editReply("❌ Invalid clan tag.").then(Mono.empty()))
                    )
                .flatMap(clan -> {
                    Mono<War> activeWar = clan.inWarLeague() ? getActiveWarLeagueWar(clan) : getActiveWar(clan);
    
                    return activeWar.flatMap(war -> SlashCommandEmbed.init(ShowEmbed.class).setViewContent(war).flatMap(embedManager -> embedManager.materializeMono())
                        .flatMap(embed -> event.editReply().withEmbeds(embed))
                        .onErrorResume(e -> {
                            log.error("Error creating war embed: {}", e.getMessage());
                            return event.editReply("❌ An error occurred while retrieving war information.");
                        }))
                        .switchIfEmpty(event.editReply("❌ No active war found for clan: " + clan.clanName()));
                })
                .onErrorResume(e -> {
                    log.error("Error handling war command: {}", e.getMessage());
                    return event.editReply("❌ Failed to fetch war information. Please try again later.");
                })
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            ).then();
    }
    


    private Mono<War> getActiveWarLeagueWar(ClanDto clan) {
        return Mono.fromCallable(() -> {
            WarLeagueGroup group;
            try {
                group = clashAPI.getWarLeagueGroup(clan.clanId());
            }
            catch (IOException | ClashAPIException e){
                return null;
            }
            if (group == null || group.getState() == WarLeagueGroupState.GROUP_NOT_FOUND) {
                return null;
            }

            List<WarLeagueRound> rounds = group.getRounds();
            for (WarLeagueRound round : rounds) {
                for (String warTag : round.getWarTags()) {
                    if (warTag.equals("#0")) continue;
                    try {
                        War war = clashAPI.getWarLeagueWar(warTag);
                        if (war != null && war.getClan().getTag().equals(clan.clanId()) &&
                            (war.getState() == WarState.IN_WAR || war.getState() == WarState.PREPARATION)) {
                            return war;
                        }
                    } catch (IOException | ClashAPIException e) {
                        log.warn("Error fetching league war for tag: " + warTag, e);
                    }
                }
            }
            return null;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
          .flatMap(war -> war != null ? Mono.just(war) : Mono.empty());
    }

    /**
     * Asynchronously retrieves the current war for a given clan.
     *
     * @param clan the ClanDto representing the clan for which to fetch the war.
     * @return a Mono containing the current War object.
     */
    private Mono<War> getActiveWar(ClanDto clan) {
        return Mono.fromCallable(() -> {
            War normalWar;
            WarState state;
            try {
                normalWar = clashAPI.getCurrentWar(clan.clanId());
                state = normalWar.getState();
            } catch (IOException | ClashAPIException e) {
                return null;
            }
            if (state != WarState.CLAN_NOT_FOUND && state != WarState.ACCESS_DENIED && state != WarState.NOT_IN_WAR) {
                return normalWar;
            }
            return null;
            
        }).flatMap(war -> war != null ? Mono.just(war) : Mono.empty());
    }
}
