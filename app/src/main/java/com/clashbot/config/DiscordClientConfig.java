package com.clashbot.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.clashbot.database.ClashBotApiClient;
import com.clashbot.discordbot.commands.CommandsCommand;
import com.clashbot.discordbot.commands.DefaultCommand;
import com.clashbot.discordbot.commands.HelpCommand;
import com.clashbot.discordbot.commands.RegisterCommand;
import com.clashbot.discordbot.commands.RegistrationsCommand;
import com.clashbot.discordbot.commands.SettingsCommand;
import com.clashbot.discordbot.commands.ShowCommand;
import com.clashbot.discordbot.commands.SlashCommand;
import com.clashbot.discordbot.commands.UnregisterCommand;
import com.clashbot.discordbot.events.AutoCompleteListener;
import com.clashbot.discordbot.events.EventListener;
import com.clashbot.discordbot.events.GuildCreateListener;
import com.clashbot.discordbot.events.GuildDeleteListener;
import com.clashbot.discordbot.events.SlashCommandListener;
import com.clashbot.service.utils.PermissionCheck;
import com.lycoon.clashapi.core.ClashAPI;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;

@Configuration
public class DiscordClientConfig {

    private final ClashBotApiClient apiClient;
    private final ClashAPI clashApi;
    private final PermissionCheck permissionCheck;
    private String discordToken;


    public DiscordClientConfig(ClashBotApiClient apiClient,
        PermissionCheck permissionCheck,
        @Value("${clash.api.email}") String capiEmail,
        @Value("${clash.api.password}") String capiPassword,
        @Value("${discord.token}") String discordToken) {
        this.apiClient   = apiClient;
        this.clashApi    = new ClashAPI(capiEmail, capiPassword);
        this.discordToken= discordToken;
        this.permissionCheck = permissionCheck;
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {

        // Modern slash commands
        List<SlashCommand> slashCommands = new ArrayList<>();
        slashCommands.add(new RegisterCommand(apiClient, clashApi, permissionCheck));
        slashCommands.add(new UnregisterCommand(apiClient, permissionCheck));
        slashCommands.add(new RegistrationsCommand(apiClient));
        slashCommands.add(new SettingsCommand(apiClient, permissionCheck));
        slashCommands.add(new ShowCommand(apiClient, clashApi));
        slashCommands.add(new DefaultCommand(apiClient, permissionCheck));
        slashCommands.add(new CommandsCommand(slashCommands));
        slashCommands.add(new HelpCommand(slashCommands));

        // Start Discord
        DiscordClient client = DiscordClient.create(discordToken);
        GatewayDiscordClient gateway = client.login().block();

        // Register slash commands with Discord (guild-level for development)
        long applicationId = client.getApplicationId().block();
        
        
        //Dev -----------------------------------
        // long guildId = 463940874611458049L;
        // client.getApplicationService()
        //     .bulkOverwriteGuildApplicationCommand(
        //         applicationId,
        //         guildId,
        //         slashCommands.stream()
        //                     .map(SlashCommand::getCommandRequest)
        //                     .toList()
        //     )
        //     .subscribe();
        //---------------------------------------



        // Prod ---------------------------------------
        client.getApplicationService()
            .bulkOverwriteGlobalApplicationCommand(
                applicationId,
                slashCommands.stream()
                            .map(SlashCommand::getCommandRequest)
                            .toList()
            )
            .subscribe();
        //---------------------------------------

        // Register event listeners
        List<EventListener<?>> listeners = List.of(
            new GuildCreateListener(apiClient),
            new GuildDeleteListener(apiClient),
            new SlashCommandListener(slashCommands),
            new AutoCompleteListener(apiClient, slashCommands)
        );

        registerListeners(gateway, listeners);

        return gateway;

    }

    @SuppressWarnings("unchecked")
    private static void registerListeners(
        GatewayDiscordClient gateway,
        List<EventListener<?>> listeners
    ) {
        for (EventListener<?> raw : listeners) {
            var eventType = (Class<discord4j.core.event.domain.Event>) raw.getEventType();
            var listener  = (EventListener<discord4j.core.event.domain.Event>) raw;
            gateway.on(eventType)
                   .flatMap(listener::onEvent)
                   .subscribe();
        }
    }
}
