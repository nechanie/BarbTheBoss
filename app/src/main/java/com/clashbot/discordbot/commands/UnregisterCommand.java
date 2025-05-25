package com.clashbot.discordbot.commands;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.UnregisterEmbed;
import com.clashbot.service.utils.PermissionCheck;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class UnregisterCommand implements SlashCommand {

    private final ClashBotApiClient api;
    private final PermissionCheck permissionCheck;

    public UnregisterCommand(ClashBotApiClient api, PermissionCheck permissionCheck) {
        this.api = api;
        this.permissionCheck = permissionCheck;
    }

    @Override
    public String getName() {
        return "unregister";
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(UnregisterEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
            .name("unregister")
            .description("Unregister a clan or account")

            .addOption(ApplicationCommandOptionData.builder()
                .name("clan")
                .description("Unregister a clan")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("clan_tag")
                    .description("The tag of the clan to unregister")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .autocomplete(true)
                    .build()
                )
                .build()
            )
            .addOption(ApplicationCommandOptionData.builder()
                .name("account")
                .description("Unregister an account")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("account_tag")
                    .description("The tag of the account to unregister")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
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

        String tagOption = subCommandName.equals("clan") ? "clan_tag" : "account_tag";

        String tag = event.getOption(subCommandName)
            .flatMap(opt -> opt.getOption(tagOption))
            .flatMap(opt -> opt.getValue().map(v -> v.asString()))
            .orElse("");

        return switch (subCommandName) {
            case "clan" -> handleClan(tag, event);
            case "account" -> handleAccount(tag, event);
            default -> event.reply("❌ Unknown type `" + subCommandName + "`. Use clan or account.")
                           .withEphemeral(true)
                           .then();
        };
    }

    private Mono<Void> handleClan(String tag, ChatInputInteractionEvent event) {
        String serverId;
        try {
        serverId = event.getInteraction().getGuildId()
            .map(Snowflake::asString)
            .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
        
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }

        return event.deferReply().withEphemeral(true)
            .then(api.getServerClanMapByCombination(serverId, tag))
            .flatMap(existingMap ->
                permissionCheck.checkServerClanPermission(
                    event.getInteraction().getMember().get().asFullMember(), existingMap)
                .filter(Boolean::booleanValue)
                .flatMap(__ ->
                    api.deleteServerClanMap(existingMap.id().intValue())
                        .then(event.editReply()
                            .withEmbeds(
                                SlashCommandEmbed.init(UnregisterEmbed.class)
                                    .setViewContent(existingMap)
                                    .materialize()
                            )
                        )
                )
                .switchIfEmpty(
                    event.editReply("❌ You don’t have permission to unregister this clan.")
                )
            )
            .switchIfEmpty(
                event.editReply("ℹ️ No registered clan found with tag `" + tag + "`.")
            )
            .onErrorResume(e ->
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            )
            .then();
    }

    private Mono<Void> handleAccount(String tag, ChatInputInteractionEvent event) {
        String discordId = event.getInteraction().getUser().getId().asString();

        return event.deferReply().withEphemeral(true)
            .then(api.getAccountMap(discordId, tag))
            .flatMap(existingMap ->
                api.deleteAccountMap(existingMap.id().intValue())
                    .then(event.editReply()
                        .withEmbeds(
                            SlashCommandEmbed.init(UnregisterEmbed.class)
                                .setViewContent(existingMap)
                                .materialize()
                        )
                    )
            )
            .switchIfEmpty(
                event.editReply("ℹ️ No account with tag `" + tag + "` is registered to you.")
            )
            .onErrorResume(e ->
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            )
            .then();
    }
}
