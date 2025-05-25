package com.clashbot.discordbot.commands;

import java.time.Duration;
import java.util.Optional;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.AccountSettingsEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.ClanSettingsEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;
import com.clashbot.models.utils.ServerSettingsUpdateRequestContainer;
import com.clashbot.service.utils.PermissionCheck;
import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.AccountSettingsDto;
import com.clashbotbackend.dto.ServerClanMapDto;
import com.clashbotbackend.dto.ServerSettingsDto;
import com.clashbotbackend.dto.UpdateAccountSettingsRequest;
import com.clashbotbackend.dto.WarNewsSettingsDto;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class SettingsCommand implements SlashCommand {

    @Override
    public String getName() {
        return "settings";
    }

    private final ClashBotApiClient api;
    private PermissionCheck permissionCheck;

    public SettingsCommand(ClashBotApiClient api, PermissionCheck permissionCheck) {
        this.api = api;
        this.permissionCheck = permissionCheck;
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(
                SlashCommandEmbed.init(ClanSettingsEmbed.class).setHelp().materialize(), 
                SlashCommandEmbed.init(AccountSettingsEmbed.class).setHelp().materialize()
            )
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
        .name("settings")
        .description("View or edit settings.")
        
        // View Subcommand Group
        .addOption(ApplicationCommandOptionData.builder()
            .name("view")
            .description("View settings.")
            .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
            
            // View Clan Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("clan")
                .description("View clan settings.")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("clan_tag")
                    .description("The clan tag to view settings for.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .autocomplete(true)
                    .build())
                .build())
            
            // View Account Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("account")
                .description("View account settings.")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("account_tag")
                    .description("The account tag to view settings for.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .autocomplete(true)
                    .build())
                .build())
            .build())

        // Edit Subcommand Group
        .addOption(ApplicationCommandOptionData.builder()
            .name("edit")
            .description("Edit settings.")
            .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
            
            // Edit Clan Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("clan")
                .description("Edit clan settings.")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("clan_tag")
                    .description("The clan tag to edit settings for.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .autocomplete(true)
                    .build())
                .build())

            // Edit Account Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("account")
                .description("Edit account settings.")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("account_tag")
                    .description("The account tag to edit settings for.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .autocomplete(true)
                    .build())
                .build())
            .build())
        
        .build();
    }


    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Get the subcommand group (view/edit) and the subcommand (clan/account)
        String group = event.getOptions().get(0).getName(); // view or edit
        String type = event.getOptions().get(0).getOptions().get(0).getName(); // clan or account
    
        // Determine the tag based on the subcommand type (clan_tag or account_tag)
        String tagOption = type.equals("clan") ? "clan_tag" : "account_tag";
        Optional<String> tag = event.getOption(group)  // view or edit
            .flatMap(opt -> opt.getOption(type))        // clan or account
            .flatMap(opt -> opt.getOption(tagOption))   // clan_tag or account_tag
            .flatMap(opt -> opt.getValue().map(v -> v.asString()));
    
        // Handle the logic based on group and type
        if (group.equalsIgnoreCase("view")) {
            if (type.equalsIgnoreCase("clan")) {
                return handleViewClan(event, tag.isPresent() ? tag.get() : null);
            } else if (type.equalsIgnoreCase("account")) {
                return handleViewAccount(event, tag);
            }
        } else if (group.equalsIgnoreCase("edit")) {
            if (type.equalsIgnoreCase("clan")) {
                return handleEditClan(event, tag);
            } else if (type.equalsIgnoreCase("account")) {
                return handleEditAccount(event, tag);
            }
        }
    
        return event.reply("Invalid command usage.").withEphemeral(true);
    }

    private Mono<Void> handleViewAccount(ChatInputInteractionEvent event, Optional<String> tag){
        if (!tag.isPresent()){
            throw new IllegalStateException("❌ Unabled to parse account_tag, make sure you provide an account_tag.");
        }

        String discordId = event.getInteraction().getUser().getId().asString();

        return event.deferReply(InteractionCallbackSpec.create().withEphemeral(true))
            .then(
                api.getAccountMap(discordId, tag.get())
                .switchIfEmpty(
                    event.editReply("❌ Unable to find registration for a account with tag: `" + tag.get() + "`.")
                        .then(Mono.empty())
                )
                .flatMap(accountMap -> 
                    event.editReply().withEmbeds(SlashCommandEmbed.init(AccountSettingsEmbed.class).setViewContent(accountMap).materialize())
                ).then()
            );
    }

    private Mono<Void> handleEditAccount(ChatInputInteractionEvent event, Optional<String> tag){
        if (!tag.isPresent()){
            throw new IllegalStateException("❌ Unabled to parse account_tag, make sure you provide an account_tag.");
        }
        return handleEditAccount(event, tag.get(), new UpdateAccountSettingsRequest(), null);
    }

    private Mono<Void> handleEditAccount(ChatInputInteractionEvent event, String tag, UpdateAccountSettingsRequest req, Mono<AccountMapDto> optionalMap){

        String discordId = event.getInteraction().getUser().getId().asString();

        UpdateAccountSettingsRequest updateReq = req;

        Mono<Void> init;

        Mono<AccountSettingsDto> settingsMono;
        Mono<AccountMapDto> mapMono;
        if (optionalMap != null){
            mapMono = optionalMap;
            settingsMono = optionalMap.flatMap(map -> Mono.just(map.accountSettings()));
            init = event.editReply(optionalMap.toString()).withEmbeds().withComponents().then();
        }
        else {
            mapMono = api.getAccountMap(discordId, tag).switchIfEmpty(Mono.error(new IllegalStateException(
                    "❌ No registered account found with tag `" + tag + "`."
                )));
            settingsMono = mapMono.flatMap(map -> Mono.just(map.accountSettings()));
            init = event.deferReply(InteractionCallbackSpec.create().withEphemeral(true));
        }

        Mono<AccountSettingsDto> cachedSettingsMono = settingsMono.cache();
        Mono<AccountMapDto> cachedMapMono = mapMono.cache();

        return init
            .then(cachedSettingsMono.flatMap(currentSettings ->{
                AccountSettingsEmbed embedBuilder = new AccountSettingsEmbed();
                return cachedMapMono.flatMap(cachedMap -> 
                    event.editReply()
                        .withEmbeds(embedBuilder.setViewContent(cachedMap).materialize(), embedBuilder.getEditEmbed())
                        .withComponents(embedBuilder.getEditComponents())
                );
            }))
            .then(
                event.getClient().on(SelectMenuInteractionEvent.class)
                    .filter(selectEvent -> selectEvent.getCustomId().startsWith("option-"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to choose a setting.").withEmbeds().withComponents()
                        .then(Mono.empty())
                    )
            )
            .flatMap(optionEvent -> {
                optionEvent.deferEdit().withEphemeral(true).subscribe();
                return cachedSettingsMono.flatMap(currentSettings ->
                    AccountSettingsEmbed.selectHandler(optionEvent, currentSettings)
                );
            })
            .flatMap(components -> 
                event.editReply().withComponents(components)
            )
            .flatMap(msg ->
                event.getClient().on(SelectMenuInteractionEvent.class)
                    .filter(selectEvent -> selectEvent.getCustomId().startsWith("value-"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to choose a setting.").withEmbeds().withComponents()
                        .then(Mono.empty())
                    )
            )
            .flatMap(valueEvent -> {
                valueEvent.deferEdit().withEphemeral(true).subscribe();

                String value = valueEvent.getValues().get(0);
                String settingId = valueEvent.getCustomId();
                return AccountSettingsEmbed.settingsChangeHandler(settingId, value.equals("true"), updateReq);
            })
            .flatMap(currentUpdateReq -> 
                event.editReply()
                    .withEmbeds(AccountSettingsEmbed.getVerificationEmbed())
                    .withComponents(AccountSettingsEmbed.getVerificationComponents())
                    .thenReturn(currentUpdateReq)
            )
            .flatMap(currentUpdateReq -> 
                event.getClient().on(ButtonInteractionEvent.class)
                    .filter(btn -> btn.getCustomId().equals("yes-changes") || btn.getCustomId().equals("no-changes"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to confirm changes.").then(Mono.empty()))
                    .flatMap(btn -> {
                        btn.deferEdit().withEphemeral(true).subscribe();

                        if (btn.getCustomId().equals("yes-changes")){
                            return cachedSettingsMono.flatMap(currentSettings -> {
                                AccountSettingsDto newSettings = new AccountSettingsDto(
                                    currentSettings.id(),
                                    currentUpdateReq.getAllowMentions() != null ? currentUpdateReq.getAllowMentions() : currentSettings.allowMentions()
                                );

                                
                                return handleEditAccount(
                                    event,
                                    null,
                                    currentUpdateReq,
                                    cachedMapMono.flatMap(temp -> 
                                        Mono.just(
                                            new AccountMapDto(
                                                temp.id(),
                                                temp.discordAccount(),
                                                temp.clashAccount(),
                                                newSettings
                                            )
                                        )
                                    )
                                );
                            });
                        }
                        else {
                            return cachedSettingsMono.flatMap(currentSettings -> 
                                api.updateAccountSettings(currentSettings.id(), currentUpdateReq)
                            )
                            .then(event.editReply("✅ Settings have been updated.").withComponents().withEmbeds()).then();
                        }
                    })
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).then()
            );
    }

    private Mono<Void> handleViewClan(ChatInputInteractionEvent event, String clanTag) {
        String serverId;
        try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }

        Mono<ServerClanMapDto> mapMono;

        if (clanTag != null) {
            mapMono = api.getServerClanMapByCombination(serverId, clanTag)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "❌ No registered clan found with tag `" + clanTag + "` in this server."
                )));
        } else {
            mapMono = api.getDefaultServerClanMap(serverId)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                    "❌ No default clan registered for this server."
                )));
        }

        return mapMono
            .flatMap(map -> event.reply()
                .withEmbeds(SlashCommandEmbed.init(ClanSettingsEmbed.class).setViewContent(map).materialize())
                .withEphemeral(true))
            .onErrorResume(e -> event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
                .withEphemeral(true));
    }


    private Mono<Void> handleEditClan(ChatInputInteractionEvent event, Optional<String> clanTag){
        if (!clanTag.isPresent()){
            return handleEditClan(event, null, new ServerSettingsUpdateRequestContainer(), null);
        }
        return handleEditClan(event, clanTag.get(), new ServerSettingsUpdateRequestContainer(), null);
    }

    private Mono<Void> handleEditClan(ChatInputInteractionEvent event, String clanTag, ServerSettingsUpdateRequestContainer req, Mono<ServerClanMapDto> optionalMap) {
        String serverId;
        try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }
    
        ServerSettingsUpdateRequestContainer updateReq = req;
        
        Mono<?> init;
        
        Mono<ServerSettingsDto> settingsMono;
        Mono<ServerClanMapDto> mapMono;
        Mono<ServerSettingsDto> cachedSettingsMono;
        Mono<ServerClanMapDto> cachedMapMono;
        if (optionalMap != null) {
            mapMono = optionalMap;
            settingsMono = optionalMap.flatMap(map -> Mono.just(map.serverSettings()));
            init = Mono.just(event);
            cachedSettingsMono = settingsMono.cache();
            cachedMapMono = mapMono.cache();
        } else {
            if (clanTag != null) {
                mapMono = api.getServerClanMapByCombination(serverId, clanTag).switchIfEmpty(Mono.error(new IllegalStateException(
                        "❌ No registered clan found with tag `" + clanTag + "` in this server."
                    )));
            }
            else {
                mapMono = api.getDefaultServerClanMap(serverId).switchIfEmpty(Mono.error(new IllegalStateException(
                    "❌ No default clan registered for this server."
                )));
            }
            settingsMono = mapMono.flatMap(map -> Mono.just(map.serverSettings()));
            cachedSettingsMono = settingsMono.cache();
            cachedMapMono = mapMono.cache();
            init = event.deferReply(InteractionCallbackSpec.create().withEphemeral(true))
            .then(
                cachedMapMono.flatMap(map ->
                    permissionCheck.checkServerClanPermission(event.getInteraction().getMember().get().asFullMember(), map)
                    .filter(Boolean::booleanValue)
                    .single()
                    .onErrorResume(e -> event.editReply("❌ You do not have the necessary permissions to do this. You must have `Manage Server` permissions in the current Discord server, or be at least a co-leader in the specified clan.")
                        .then(Mono.empty())
                    )
                )
            );
        }
    
        return init
            // initial embedding with option select
            .flatMap(__ -> cachedSettingsMono.flatMap(currentSettings ->{
                ClanSettingsEmbed embedBuilder = new ClanSettingsEmbed();
                return cachedMapMono.flatMap(cachedMap ->
                    event.editReply()
                        .withEmbeds(embedBuilder.setViewContent(cachedMap).materialize(), embedBuilder.getEditEmbed())
                        .withComponents(embedBuilder.getEditComponents(currentSettings.warNewsNotifications()))
                );
            }))
            // wait for option selection event
            .flatMap(__2 ->
                event.getClient().on(SelectMenuInteractionEvent.class)
                    .filter(selectEvent -> selectEvent.getCustomId().startsWith("option-"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to choose a setting.").withEmbeds().withComponents()
                        .then(Mono.empty())
                    )
            )
            // handle option selection event
            .flatMap(optionEvent ->{
                optionEvent.deferEdit().withEphemeral(true).subscribe();
                return cachedSettingsMono.flatMap(currentSettings ->
                    ClanSettingsEmbed.selectHandler(optionEvent, currentSettings)
                );
            })
            // swap out option selection for value selection
            .flatMap(components ->
                event.editReply().withComponents(components)
            )
            // wait for value selection event
            .flatMap(msg ->
                event.getClient().on(SelectMenuInteractionEvent.class)
                    .filter(selectEvent -> selectEvent.getCustomId().startsWith("value-"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to select a value.").withComponents().withEmbeds()
                        .then(Mono.empty())
                    )
            )
            // handle value selection event
            .flatMap(valueEvent -> {
                valueEvent.deferEdit().withEphemeral(true).subscribe(); // Fire-and-forget
    
                String value = valueEvent.getValues().get(0);
                String settingId = valueEvent.getCustomId();
                return ClanSettingsEmbed.settingsChangeHandler(settingId, value.equals("true"), updateReq);
            })
            // replace edit embed with verification embed
            .flatMap(currentUpdateReq ->
                Mono.zip(cachedMapMono, cachedSettingsMono)
                    .flatMap(tuple -> {
                        ServerClanMapDto cachedMap = tuple.getT1();
                        ServerSettingsDto currentSettings = tuple.getT2();
                        ServerSettingsDto newSettings = new ServerSettingsDto(
                            currentSettings.id(),
                            currentUpdateReq.getUpdateServerSettingsRequest().getAttackNotification() != null ? currentUpdateReq.getUpdateServerSettingsRequest().getAttackNotification() : currentSettings.attackNotification(),
                            currentUpdateReq.getUpdateServerSettingsRequest().getWarNewsNotifications() != null ? currentUpdateReq.getUpdateServerSettingsRequest().getWarNewsNotifications() : currentSettings.warNewsNotifications(),
                            currentUpdateReq.getUpdateServerSettingsRequest().getWarSignupReminder() != null ? currentUpdateReq.getUpdateServerSettingsRequest().getWarSignupReminder() : currentSettings.warSignupReminder(),
                            new WarNewsSettingsDto(
                                currentSettings.warNewsSettings().id(),
                                currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnLeadChange() != null ? currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnLeadChange() : currentSettings.warNewsSettings().onLeadChange(),
                                currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnAllyThreeStarAttack() != null ? currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnAllyThreeStarAttack() : currentSettings.warNewsSettings().onAllyThreeStarAttack(),
                                currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnPerfectWar() != null ? currentUpdateReq.getUpdateWarNewsSettingsRequest().isOnPerfectWar() : currentSettings.warNewsSettings().onPerfectWar()
                            ),
                            currentSettings.notificationsEnabled()
                        );
                        ServerClanMapDto newMap = new ServerClanMapDto(
                                                cachedMap.id(),
                                                cachedMap.server(),
                                                cachedMap.clan(),
                                                newSettings,
                                                cachedMap.defaultServerClan());
                        return event.editReply()
                        .withEmbeds(SlashCommandEmbed.init(ClanSettingsEmbed.class).setViewContent(newMap).materialize(), ClanSettingsEmbed.getVerificationEmbed())
                        .withComponents(ClanSettingsEmbed.getVerificationComponents())
                        .thenReturn(newMap);
                    })
            )       
            // wait for verification event     
            .flatMap(updatedMap ->
                event.getClient().on(ButtonInteractionEvent.class)
                    .filter(btn -> btn.getCustomId().equals("yes-changes") || btn.getCustomId().equals("no-changes"))
                    .next()
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> event.editReply("⚠️ You took too long to confirm changes.").withComponents().withEmbeds().then(Mono.empty()))
                    .flatMap(btn -> {
                        btn.deferEdit().withEphemeral(true).subscribe(); // Fire-and-forget
    
                        if (btn.getCustomId().equals("yes-changes")) {
                            return cachedSettingsMono.flatMap(currentSettings -> {
                                // Use a loop-back handler only once more – avoid infinite recursion
                                return handleEditClan(
                                    event,
                                    null,
                                    updateReq,
                                    cachedMapMono.flatMap(temp ->
                                        Mono.just(
                                            updatedMap
                                        )
                                    )
                                );
                            });
                        } else {
                            return cachedSettingsMono.flatMap(currentSettings ->
                                api.updateServerSettings(currentSettings.id(), updateReq.getUpdateServerSettingsRequest())
                                    .then(
                                        api.updateWarNewsSettings(currentSettings.warNewsSettings().id(), updateReq.getUpdateWarNewsSettingsRequest())
                                    )
                            )
                            .then(event.editReply("✅ Settings have been updated.").withComponents().withEmbeds()).then();
                        }
                    })
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).then()
            );
    }
}
