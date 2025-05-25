package com.clashbot.discordbot.commands;

import java.util.Optional;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.embeds.CommandEmbeds.DefaultEmbed;
import com.clashbot.discordbot.embeds.CommandEmbeds.SlashCommandEmbed;
import com.clashbot.service.utils.PermissionCheck;
import com.clashbotbackend.dto.ServerClanMapDto;
import com.clashbotbackend.dto.ServerDto;
import com.clashbotbackend.dto.UpdateServerClanMapRequest;
import com.clashbotbackend.dto.UpdateServerRequest;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class DefaultCommand implements SlashCommand {
    
    private final ClashBotApiClient api;
    private final PermissionCheck permissionCheck;

    public DefaultCommand(ClashBotApiClient api, PermissionCheck permissionCheck){
        this.api = api;
        this.permissionCheck = permissionCheck;
    }

    @Override
    public String getName(){
        return "default";
    }

    @Override
    public Mono<Void> help(ChatInputInteractionEvent event){
        return event.reply(
            InteractionApplicationCommandCallbackSpec.
            create()
            .withEphemeral(true)
            .withEmbeds(SlashCommandEmbed.init(DefaultEmbed.class).setHelp().materialize())
        );
    }

    @Override
    public ApplicationCommandRequest getCommandRequest(){
        return ApplicationCommandRequest.builder()
            .name(getName())
            .description("Operations related to default bot values.")
            .addOption(
                ApplicationCommandOptionData.builder()
                .name("channel")
                .description("Default Text Channel for Automated Messages")
                .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                .addOption(ApplicationCommandOptionData.builder()
                    .name("edit")
                    .description("Change the current default text channel.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .addOption(
                        ApplicationCommandOptionData.builder()
                        .name("channel_choice")
                        .description("A visible text channel in this discord server to set as the default")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .autocomplete(true)
                        .required(true)
                        .build()
                    )
                    .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                    .name("view")
                    .description("View the current default text channel.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .build()
                )
                .build()
            )
            .addOption(
                ApplicationCommandOptionData.builder()
                .name("clan")
                .description("Default clan for to this server. (The default clan is used for commands when no `clan_tag` is given)")
                .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                .addOption(
                    ApplicationCommandOptionData.builder()
                    .name("edit")
                    .description("Change the current default clan.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .addOption(
                        ApplicationCommandOptionData.builder()
                        .name("clan_tag")
                        .description("The clan_tag for a clan registered to this server to set as default.")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .autocomplete(true)
                        .required(true)
                        .build()
                    )
                    .build()   
                )
                .addOption(
                    ApplicationCommandOptionData.builder()
                    .name("view")
                    .description("View the current default clan.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .build()   
                )
                .build()
            ).build();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        // Get the group: "channel" or "clan"
        Optional<ApplicationCommandInteractionOption> groupOpt = event.getOption("channel")
            .or(() -> event.getOption("clan"));

        if (groupOpt.isEmpty()) {
            return event.reply("❌ Missing command group (channel or clan).").withEphemeral(true);
        }

        String groupName = groupOpt.get().getName(); // "channel" or "clan"
        Optional<ApplicationCommandInteractionOption> subCommandOpt = groupOpt.get().getOptions().stream().findFirst();

        if (subCommandOpt.isEmpty()) {
            return event.reply("❌ Missing subcommand (view or edit).").withEphemeral(true);
        }

        String subCommandName = subCommandOpt.get().getName(); // "view" or "edit"
        ApplicationCommandInteractionOption subCommand = subCommandOpt.get();

        // Handle /default channel view or edit
        if (groupName.equalsIgnoreCase("channel")) {
            if (subCommandName.equalsIgnoreCase("view")) {
                return handleViewDefaultChannel(event);
            } else if (subCommandName.equalsIgnoreCase("edit")) {
                return subCommand.getOption("channel_choice")
                    .flatMap(opt -> opt.getValue())
                    .map(v -> v.asChannel())
                    .orElse(Mono.empty())
                    .flatMap(channel -> handleEditDefaultChannel(event, channel));
            }
        }

        // Handle /default clan view or edit
        if (groupName.equalsIgnoreCase("clan")) {
            if (subCommandName.equalsIgnoreCase("view")) {
                return handleViewDefaultClan(event);
            } else if (subCommandName.equalsIgnoreCase("edit")) {
                return subCommand.getOption("clan_tag")
                    .flatMap(opt -> opt.getValue().map(v -> v.asString()))
                    .map(clanTag -> handleEditDefaultClan(event, clanTag))
                    .orElse(event.reply("❌ Missing `clan_tag` value.").withEphemeral(true));
            }
        }

        return event.reply("❌ Invalid command usage.").withEphemeral(true);
    }


    private Mono<Void> handleViewDefaultChannel(ChatInputInteractionEvent event){
        String serverId;
        try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }
        
        Mono<ServerDto> server = api.getServerById(serverId); 
        
        return event.getInteraction().getGuild()
            .flatMap(guild -> 
                server.flatMap(thisServer -> {
                    if (thisServer.defaultTextChannel() != null){
                        return guild.getChannelById(Snowflake.of(thisServer.defaultTextChannel())).cast(TextChannel.class)
                        .onErrorResume(e -> 
                            event.reply("❌ This server has no default channel.").withEphemeral(true).then(Mono.empty())
                            );
                    }
                    return event.reply("❌ This server has no default channel.").withEphemeral(true).then(Mono.empty());
                })
                .flatMap(textChannel -> 
                    SlashCommandEmbed.init(DefaultEmbed.class).setViewContent(guild.getName(), textChannel).materializeMono()
                    .flatMap(embed -> 
                        event.reply().withEphemeral(true).withEmbeds(embed)
                    )
                )
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).then()
            ).then();
    }

    private Mono<Void> handleEditDefaultChannel(ChatInputInteractionEvent event, Channel channel) {
        String serverId;
        try {
            serverId = event.getInteraction().getGuildId()
                .map(Snowflake::asString)
                .orElseThrow(() -> new IllegalStateException("❌ Command must be used inside of a discord server."));
            
        } catch (Exception e) {
            return event.reply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."));
        }

        Mono<ServerDto> serverMono = api.getServerById(serverId);

        UpdateServerRequest req = new UpdateServerRequest();
        req.setDefaultTextChannel(channel.getId().asString());

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
            .flatMap(__ -> serverMono.flatMap(currentServer -> {
                return api.updateServer(currentServer.serverId(), req)
                    .flatMap(updatedServer ->
                        event.getInteraction().getGuild().flatMap(guild -> {
                            Mono<Optional<GuildChannel>> oldChannelMono = Optional.ofNullable(currentServer.defaultTextChannel())
                                .map(id -> guild.getChannelById(Snowflake.of(id))
                                    .map(Optional::of)
                                    .onErrorResume(e -> {
                                        return Mono.just(Optional.empty());
                                    }))
                                .orElse(Mono.just(Optional.empty()));

                            Mono<GuildChannel> newChannelMono = guild.getChannelById(Snowflake.of(updatedServer.defaultTextChannel()));

                            return Mono.zip(oldChannelMono, newChannelMono)
                                .flatMap(tuple -> {
                                    Optional<GuildChannel> maybeOldChannel = tuple.getT1();
                                    GuildChannel newChannel = tuple.getT2();

                                    String response = maybeOldChannel
                                        .map(old -> String.format("✅ Default channel changed from `%s` to `%s`.",
                                            old.getName(), newChannel.getName()))
                                        .orElse(String.format("✅ The default channel has been set to `%s`.",
                                            newChannel.getName()));

                                    System.out.println("[Step 5] Sending response: " + response);
                                    return event.editReply(response).then();
                                });
                        })
                    );
            }))
            .onErrorResume(e -> {
                System.out.println("[Error] " + Optional.ofNullable(e.getMessage()).orElse("Unknown error"));
                return event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).then();
            });
    }


    private Mono<Void> handleViewDefaultClan(ChatInputInteractionEvent event){
        String serverId = event.getInteraction().getGuildId()
            .map(Snowflake::asString)
            .orElseThrow(() -> new IllegalStateException("❌ Could not determine your server."));
        
        Mono<ServerClanMapDto> defaultMap = api.getDefaultServerClanMap(serverId); 
        
        return event.deferReply().withEphemeral(true)
            .then(
                event.getInteraction().getGuild()
                    .flatMap(guild -> 
                        defaultMap
                            .flatMap(map -> 
                                SlashCommandEmbed.init(DefaultEmbed.class).setViewContent(guild.getName(), map.clan()).materializeMono()
                                    .flatMap(embed -> event.editReply().withEmbeds(embed))
                            )
                            .switchIfEmpty(
                                event.editReply("❌ This server has no default clan.")
                            )
                    )
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred."))
            )
            .then();
    }

    private Mono<Void> handleEditDefaultClan(ChatInputInteractionEvent event, String clanTag) {

        String serverId = event.getInteraction().getGuildId()
            .map(Snowflake::asString)
            .orElseThrow(() -> new IllegalStateException("❌ Could not determine your server."));

        Mono<Optional<ServerClanMapDto>> currentDefaultMapMono = api.getDefaultServerClanMap(serverId)
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .onErrorResume(e -> Mono.just(Optional.empty()));

        // Modify newDefaultMapMono to handle empty and fail appropriately
        Mono<ServerClanMapDto> newDefaultMapMono = api.getServerClanMapByCombination(serverId, clanTag)
            .switchIfEmpty(
                event.editReply("❌ Unable to find registration for a clan with tag: `" + clanTag + "` in this server.")
                    .then(Mono.empty())
            );

        return event.deferReply().withEphemeral(true)
            .then(
                permissionCheck.checkServerPermission(event.getInteraction().getMember().get().asFullMember())
                    .single()
                    .onErrorResume(e -> event.editReply("❌ You do not have the necessary permissions to do this. You must have `Manage Server` permissions in the current Discord server.")
                        .then(Mono.empty()))
            )
            .flatMap(__ ->
                Mono.zip(currentDefaultMapMono, newDefaultMapMono)
                    .flatMap(tuple -> {
                        Optional<ServerClanMapDto> maybeOldDefault = tuple.getT1();
                        ServerClanMapDto newDefault = tuple.getT2();

                        Mono<?> unsetOld = maybeOldDefault
                            .map(old -> {
                                UpdateServerClanMapRequest unsetReq = new UpdateServerClanMapRequest();
                                unsetReq.setDefaultServerClan(false);
                                return api.updateServerClanMap(old.id(), unsetReq);
                            })
                            .orElse(Mono.empty());

                        UpdateServerClanMapRequest setReq = new UpdateServerClanMapRequest();
                        setReq.setDefaultServerClan(true);

                        return unsetOld.then(
                            api.updateServerClanMap(newDefault.id(), setReq)
                                .flatMap(updated -> {
                                    String response = maybeOldDefault
                                        .map(old -> String.format(
                                            "✅ Default clan changed from `%s (%s)` to `%s (%s)`.",
                                            old.clan().clanName(), old.clan().clanId(),
                                            updated.clan().clanName(), updated.clan().clanId()))
                                        .orElse(String.format(
                                            "✅ The default clan has been set to `%s (%s)`.",
                                            updated.clan().clanName(), updated.clan().clanId()));

                                    return event.editReply(response).then();
                                })
                        );
                    })
            )
            .onErrorResume(e -> 
                event.editReply("❌ " + Optional.ofNullable(e.getMessage()).orElse("An unexpected error occurred.")).then()
            )
            .then();
    }

}
