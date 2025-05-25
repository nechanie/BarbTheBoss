package com.clashbot.discordbot.embeds.CommandEmbeds;

import com.clashbot.models.utils.ServerSettingsUpdateRequestContainer;
import com.clashbotbackend.dto.ServerClanMapDto;
import com.clashbotbackend.dto.ServerSettingsDto;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ClanSettingsEmbed implements SlashCommandEmbed<ClanSettingsEmbed> {

    private Builder embed = EmbedCreateSpec.builder();
    private static ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");
    
    public ClanSettingsEmbed setViewContent(ServerClanMapDto serverClanMap){
        embed.color(Color.JAZZBERRY_JAM)
            .author(resources.getString("settings.clan.author").formatted(serverClanMap.clan().clanName()), null, null)
            .description(
                resources.getString("settings.clan.description.primary")
                    .formatted(
                        serverClanMap.clan().clanName(),
                        serverClanMap.serverSettings().attackNotification() ? resources.getString("shared.enabled") : resources.getString("shared.disabled"),
                        serverClanMap.serverSettings().warSignupReminder() ? resources.getString("shared.enabled") : resources.getString("shared.disabled"),
                        serverClanMap.serverSettings().warNewsNotifications()
                            ? resources.getString("settings.clan.description.secondary.enabled").formatted(
                                serverClanMap.serverSettings().warNewsSettings().onAllyThreeStarAttack() ? resources.getString("shared.enabled") : resources.getString("shared.disabled"),
                                serverClanMap.serverSettings().warNewsSettings().onLeadChange() ? resources.getString("shared.enabled") : resources.getString("shared.disabled"),
                                serverClanMap.serverSettings().warNewsSettings().onPerfectWar() ? resources.getString("shared.enabled") : resources.getString("shared.disabled")
                            ) 
                            : resources.getString("settings.clan.description.secondary.disabled")
                    )
            )
            .timestamp(Instant.now());
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
    public ClanSettingsEmbed setHelp() {
        embed.author(resources.getString("settings.clan.help.author"), null, null)
            .title(resources.getString("settings.clan.help.title"))
            .description(resources.getString("settings.clan.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("settings.clan.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("settings.clan.help.examples.value"), false)
            .addField(resources.getString("shared.help.tip.name"), resources.getString("settings.clan.help.tip.value"), false)
            .addField(resources.getString("shared.help.legend.name"),
                resources.getString("shared.help.legend.value"),
                false)
            .color(Color.of(114, 137, 218));
        return this;
    }

    public EmbedCreateSpec getEditEmbed() {
        return EmbedCreateSpec.builder()
            .color(Color.ENDEAVOUR)
            .title(resources.getString("settings.edit.title"))
            .description(resources.getString("settings.edit.description"))
            .timestamp(Instant.now())
            .build();
    }

    public LayoutComponent getEditComponents(boolean includeWarEvents) {
        return ActionRow.of(
            SelectMenu.of("option-select", buildOptions(includeWarEvents))
                .withPlaceholder(resources.getString("settings.edit.option.placeholder"))
        );
    }

    public static EmbedCreateSpec getVerificationEmbed() {
        return EmbedCreateSpec.builder()
            .color(Color.DARK_GOLDENROD)
            .title(resources.getString("settings.verification.title"))
            .description(resources.getString("settings.verification.description"))
            .timestamp(Instant.now())
            .build();
    }

    public static LayoutComponent getVerificationComponents() {
        return ActionRow.of(
            Button.success("no-changes", resources.getString("settings.verification.confirm.label")),
            Button.danger("yes-changes", resources.getString("settings.verification.decline.label"))
        );
    }

    private List<Option> buildOptions(boolean includeWarEvents) {
        List<Option> options = new ArrayList<>();

        options.add(Option.of(resources.getString("settings.edit.clan.option.attack-reminder"), "attack-reminders"));
        options.add(Option.of(resources.getString("settings.edit.clan.option.war-signup-reminder"), "signup-reminder-sub"));
        options.add(Option.of(resources.getString("settings.edit.clan.option.war-news-updates"), "war-news-sub"));

        if (includeWarEvents) {
            options.add(Option.of(resources.getString("settings.edit.clan.option.updates.three-star-attack"), "three-star-attacks"));
            options.add(Option.of(resources.getString("settings.edit.clan.option.updates.lead-change"), "lead-change"));
            options.add(Option.of(resources.getString("settings.edit.clan.option.updates.perfect-war"), "perfect-war"));
        }

        return options;
    }


    public static Mono<LayoutComponent> selectHandler(SelectMenuInteractionEvent event, ServerSettingsDto settings) {
        if (!event.getCustomId().equals("option-select")) {
            return Mono.empty();
        }

        String selected = event.getValues().get(0);
        String customId = "value-" + selected + "-update";
        boolean currentState;

        switch (selected) {
            case "attack-reminders":
                currentState = settings.attackNotification();
                break;
            case "war-news-sub":
                currentState = settings.warNewsNotifications();
                break;
            case "signup-reminder-sub":
                currentState = settings.warSignupReminder();
                break;
            case "three-star-attacks":
                if (settings.warNewsSettings() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                currentState = settings.warNewsSettings().onAllyThreeStarAttack();
                break;
            case "lead-change":
                if (settings.warNewsSettings() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                currentState = settings.warNewsSettings().onLeadChange();
                break;
            case "perfect-war":
                if (settings.warNewsSettings() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                currentState = settings.warNewsSettings().onPerfectWar();
                break;
            default:
                return Mono.empty();
        }

        return Mono.just(ActionRow.of(
            SelectMenu.of(customId, List.of(
                Option.of(resources.getString("shared.enabled"), "true").withDefault(currentState),
                Option.of(resources.getString("shared.disabled"), "false").withDefault(!currentState)
            )).withPlaceholder("Select a new value")
        ));
    }

    public static Mono<ServerSettingsUpdateRequestContainer> settingsChangeHandler(String component, Boolean value, ServerSettingsUpdateRequestContainer reqs) {
        if (!component.startsWith("value-") || !component.endsWith("-update")) {
            return Mono.empty();
        }

        String key = component.replace("value-", "").replace("-update", "");

        switch (key) {
            case "attack-reminders":
                reqs.getUpdateServerSettingsRequest().setAttackNotification(value);
                break;
            case "war-news-sub":
                reqs.getUpdateServerSettingsRequest().setWarNewsNotifications(value);
                break;
            case "signup-reminder-sub":
                reqs.getUpdateServerSettingsRequest().setWarSignupReminder(value);
                break;
            case "three-star-attacks":
                if (reqs.getUpdateWarNewsSettingsRequest() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                reqs.getUpdateWarNewsSettingsRequest().setOnAllyThreeStarAttack(value);
                break;
            case "lead-change":
                if (reqs.getUpdateWarNewsSettingsRequest() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                reqs.getUpdateWarNewsSettingsRequest().setOnLeadChange(value);
                break;
            case "perfect-war":
                if (reqs.getUpdateWarNewsSettingsRequest() == null){
                    throw new NullPointerException("War updates sub-settings not available.");
                }
                reqs.getUpdateWarNewsSettingsRequest().setOnPerfectWar(value);
                break;
            default:
                return Mono.empty();
        }

        return Mono.just(reqs);
    }
}
