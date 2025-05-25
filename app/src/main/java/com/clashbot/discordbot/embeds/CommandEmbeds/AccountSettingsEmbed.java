package com.clashbot.discordbot.embeds.CommandEmbeds;

import java.time.Instant;
import java.util.List;
import java.util.ResourceBundle;

import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.AccountSettingsDto;
import com.clashbotbackend.dto.UpdateAccountSettingsRequest;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import discord4j.core.spec.EmbedCreateSpec.Builder;

public class AccountSettingsEmbed implements SlashCommandEmbed<AccountSettingsEmbed> {

    private Builder embed = EmbedCreateSpec.builder();
    private static ResourceBundle resources = ResourceBundle.getBundle("discordbot.commands.success");

    public AccountSettingsEmbed setViewContent(AccountMapDto accountMap) {
        embed.color(Color.JAZZBERRY_JAM)
            .author(resources.getString("settings.account.author").formatted(accountMap.discordAccount().username(), accountMap.clashAccount().username()), null, null)
            .title(resources.getString("settings.account.title"))
            .description(resources.getString("settings.account.description").formatted(accountMap.clashAccount().username()))
            .addField(
                resources.getString("settings.edit.account.option.mentions"), 
                accountMap.accountSettings().allowMentions() ? resources.getString("shared.enabled") : resources.getString("shared.disabled"), 
                false
            )
            .timestamp(Instant.now());
        return this;
    }

    @Override
    public EmbedCreateSpec materialize(){
        return embed.build();
    }

    @Override
    public Mono<EmbedCreateSpec> materializeMono(){
        return Mono.just(embed.build());
    }

    @Override
    public AccountSettingsEmbed setHelp() {
        embed.author(resources.getString("settings.account.help.author"), null, null)
            .title(resources.getString("settings.account.help.title"))
            .description(resources.getString("settings.account.help.description"))
            .addField(resources.getString("shared.help.usage.name"), resources.getString("settings.account.help.usage.value"), false)
            .addField(resources.getString("shared.help.examples.name"), resources.getString("settings.account.help.examples.value"), false)
            .addField(resources.getString("shared.help.tip.name"), resources.getString("settings.account.help.tip.value"), false)
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

    public LayoutComponent getEditComponents() {
        return ActionRow.of(
            SelectMenu.of("option-select", getOptions())
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

    private List<Option> getOptions() {
        return List.of(
            Option.of(resources.getString("settings.edit.account.option.mentions"), "allow-mentions")
        );
    }

    public static Mono<LayoutComponent> selectHandler(SelectMenuInteractionEvent event, AccountSettingsDto settings) {
        if (!event.getCustomId().equals("option-select")) {
            return Mono.empty();
        }

        String selected = event.getValues().get(0);
        String customId = "value-" + selected + "-update";
        boolean currentState;

        switch (selected) {
            case "allow-mentions":
                currentState = settings.allowMentions();
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

    public static Mono<UpdateAccountSettingsRequest> settingsChangeHandler(String component, Boolean value, UpdateAccountSettingsRequest req) {
        if (!component.startsWith("value-") || !component.endsWith("-update")) {
            return Mono.empty();
        }

        String key = component.replace("value-", "").replace("-update", "");

        switch (key) {
            case "allow-mentions":
                req.setAllowMentions(value);
                break;
            default:
                return Mono.empty();
        }

        return Mono.just(req);
    }
}
