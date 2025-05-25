package com.clashbot.discordbot.commands;

import java.util.Optional;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.RegisterEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;
import com.clashbot.service.utils.PermissionCheck;
import com.clashbotbackend.dto.AccountMapDto;
import com.clashbotbackend.dto.ClanDto;
import com.clashbotbackend.dto.ClashAccountDto;
import com.clashbotbackend.dto.CreateAccountMapRequest;
import com.clashbotbackend.dto.CreateClanRequest;
import com.clashbotbackend.dto.CreateClashAccountRequest;
import com.clashbotbackend.dto.CreateDiscordAccountRequest;
import com.clashbotbackend.dto.CreateServerClanMapRequest;
import com.clashbotbackend.dto.DiscordAccountDto;
import com.clashbotbackend.dto.ServerClanMapDto;
import com.clashbotbackend.dto.ServerDto;
import com.lycoon.clashapi.core.ClashAPI;
import com.lycoon.clashapi.models.player.Player;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class RegisterCommand implements SlashCommand {

    private final ClashBotApiClient api;
    private final ClashAPI capi;
    private final PermissionCheck permissionCheck;

    public RegisterCommand(ClashBotApiClient api, ClashAPI capi, PermissionCheck permissionCheck) {
        this.api = api;
        this.capi = capi;
        this.permissionCheck = permissionCheck;
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(RegisterEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest() {
        return ApplicationCommandRequest.builder()
            .name("register")
            .description("Register a clan or account")
            
            // Clan Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("clan")
                .description("Register a clan")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("clan_tag")
                    .description("The tag of the clan to register")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
                .build())
            
            // Account Subcommand
            .addOption(ApplicationCommandOptionData.builder()
                .name("account")
                .description("Register an account")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("account_tag")
                    .description("The tag of the account to register")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
                .build())
            
            .build();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Get the subcommand name (clan or account)
        String subCommandName = event.getOptions().get(0).getName();

        // Get the appropriate tag based on the subcommand
        String tagOption = subCommandName.equals("clan") ? "clan_tag" : "account_tag";
        String tag = event.getOption(subCommandName)
            .flatMap(opt -> opt.getOption(tagOption))
            .flatMap(opt -> opt.getValue().map(v -> v.asString()))
            .orElse("");

        return switch (subCommandName) {
            case "clan" -> handleClan(tag, event);
            case "account" -> handleAccount(tag, event);
            default -> event.reply("❌ Unknown subcommand `" + subCommandName + "`. Use clan or account.")
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
            .then(
                permissionCheck.checkServerPermission(event.getInteraction().getMember().get().asFullMember())
                .filter(Boolean::booleanValue)
                .single()
                .onErrorResume(e ->
                    event.editReply("❌ You do not have the necessary permissions to do this. You must have `Manage Server` permissions in the current Discord server.")
                        .then(Mono.empty())
                )  
            )
            .flatMap(__ -> 
                api.getServerClanMapByCombination(serverId, tag)
                .flatMap(existing ->
                    event.editReply()
                         .withEmbeds(
                             SlashCommandEmbed.init(RegisterEmbed.class)
                                 .setViewContent(existing)
                                 .materialize()
                         )
                )
                .switchIfEmpty(
                    registerNewClan(serverId, tag)
                        .flatMap(newMap ->
                            event.editReply()
                                 .withEmbeds(
                                     SlashCommandEmbed.init(RegisterEmbed.class)
                                         .setViewContent(newMap)
                                         .materialize()
                                 )
                        )
                )
                .onErrorResume(error ->
                    event.editReply("❌ Unable to register your clan. Make sure the clan tag you provided is correct.")
                )
            )
            .onErrorResume(e ->
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            )
            .then();
    }

    private Mono<Void> handleAccount(String tag, ChatInputInteractionEvent event) {
        String discordId = event.getInteraction().getUser().getId().asString();
        String username = event.getInteraction().getUser().getUsername();

        Mono<Player> playerMono = Mono.fromCallable(() -> capi.getPlayer(tag))
                    .onErrorResume(e -> event.editReply("❌ Invalid account tag").then(Mono.empty()));

        return api.getAccountMap(discordId, tag)
            .switchIfEmpty(
                // ensure ClashAccount exists
                playerMono
                    .flatMap(player -> 
                        verifyNewAccountModal(event, tag)
                        .flatMap(valueEvent -> {
                            String token = valueEvent.getComponents(TextInput.class).getFirst().getValue().orElse("");
                            return Mono.fromCallable(() -> capi.isVerifiedPlayer(player.getTag(), token)).onErrorResume(e -> Mono.just(false))
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(valueEvent.reply("❌ Verification failed. Please Try again.").withEphemeral(true).then(Mono.empty()))
                                .flatMap(__ -> 
                                    registerNewAccount(discordId, username, tag, player)
                                )
                                .flatMap(newMap -> 
                                    valueEvent.reply().withEphemeral(true)
                                        .withEmbeds(
                                            SlashCommandEmbed.init(RegisterEmbed.class)
                                            .setViewContent(newMap)
                                            .materialize()
                                        ).then(Mono.empty())
                                );
                        })
                    )
            )
            .flatMap(existing ->
                event.reply()
                    .withEmbeds(
                        SlashCommandEmbed.init(RegisterEmbed.class)
                            .setExistingViewContent(existing)
                            .materialize()
                    )
            )
            .onErrorResume(e ->
                event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).withEphemeral(true)
            )
            .then();
    }

    private Mono<ServerClanMapDto> registerNewClan(String serverId, String tag) {
        // 1) get or create the Clan
        Mono<ClanDto> clanMono = api.getClanById(tag)
            .switchIfEmpty(
                Mono.fromCallable(() -> capi.getClan(tag))
                    .onErrorResume(e -> Mono.error(new IllegalStateException("Invalid clan tag")))
                    .flatMap(remote -> {
                        CreateClanRequest req = new CreateClanRequest();
                        req.setClanId(remote.getTag());
                        req.setClanName(remote.getName());
                        return api.createClan(req);
                    })
            );

        // 2) get the Server
        Mono<ServerDto> serverMono = api.getServerById(serverId)
            .switchIfEmpty(Mono.error(new IllegalStateException(
                "❌ No server record found for ID `" + serverId + "`."
            )));

        // zip them all together, then create the mapping
        return Mono.zip(clanMono, serverMono)
            .flatMap(tuple -> {
                ClanDto dbClan                    = tuple.getT1();
                ServerDto server                  = tuple.getT2();
                
                return api.listAllServerClanMapsByDiscord(serverId).collectList().flatMap(maps ->
                    Mono.just(maps.isEmpty())
                )
                .flatMap(isDefault ->{
                    CreateServerClanMapRequest req = new CreateServerClanMapRequest();
                    req.setServerId(server.serverId());
                    req.setClanId(dbClan.clanId());
                    req.setDefaultServerClan(isDefault);
                    return api.createServerClanMap(req);
                });
            });
    }

    private Mono<ModalSubmitInteractionEvent> verifyNewAccountModal(ChatInputInteractionEvent event, String tag){
        return event.presentModal(RegisterEmbed.getVerificationModal())
            .then(
                event.getClient().on(ModalSubmitInteractionEvent.class)
                    .filter(modalEvent -> modalEvent.getCustomId().equals("account-verification-modal"))
                    .next()
            );
    }

    
    private Mono<AccountMapDto> registerNewAccount(String discordId,
                                            String discordUsername,
                                            String tag, Player player) {
        // 1) ensure ClashAccount exists
        Mono<ClashAccountDto> clashMono = api.getClashAccount(tag)
            .switchIfEmpty(
                Mono.just(player)
                    .map(playerContent -> {
                        CreateClashAccountRequest req = new CreateClashAccountRequest();
                        req.setClashId(playerContent.getTag());
                        req.setUsername(playerContent.getName());
                        req.setClanId(playerContent.getClan().getTag());
                        req.setLeader(
                            playerContent.getRole().toString().equalsIgnoreCase("coLeader")
                            || playerContent.getRole().toString().equalsIgnoreCase("leader")
                        );
                        return req;
                    })
                    .flatMap(api::createClashAccount)
            );

        // 2) ensure DiscordAccount exists
        Mono<DiscordAccountDto> discordMono = api.getDiscordAccount(discordId)
        .switchIfEmpty(
            Mono.fromCallable(() -> {
                CreateDiscordAccountRequest req = new CreateDiscordAccountRequest();
                req.setDiscordId(discordId);
                req.setUsername(discordUsername);
                return req;
            }).flatMap(req -> api.createDiscordAccount(req))
        );

        // 3) link them
        return Mono.zip(clashMono, discordMono)
            .flatMap(tuple -> {
                ClashAccountDto ca = tuple.getT1();
                DiscordAccountDto da = tuple.getT2();
                CreateAccountMapRequest req = new CreateAccountMapRequest();
                req.setClashAccountId(ca.clashId());
                req.setDiscordAccountId(da.discordId());
                return api.createAccountMap(req);
            });
    }
}