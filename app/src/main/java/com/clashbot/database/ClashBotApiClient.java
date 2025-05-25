package com.clashbot.database;

import com.clashbotbackend.dto.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A non‐blocking WebClient‐backed client for all ClashBot REST endpoints.
 * <p>
 * Each method corresponds to one of the service’s API routes and returns
 * a Reactor {@link Mono} or {@link Flux} of the desired DTO/model.
 */
@Component
public class ClashBotApiClient {

    private final WebClient http;

    /**
     * Create a new API client that will call all routes under the configured base‐URL.
     *
     * @param http a {@link WebClient} preconfigured with the ClashBot API base URL
     */
    public ClashBotApiClient(@Qualifier("clashbotApiClient") WebClient http) {
        this.http = http;
    }

    // ─── Servers ────────────────────────────────────────────────────────────────

    /**
     * Retrieve all registered servers.
     *
     * @return a {@link Flux} stream of all {@link ServerDto} instances
     */
    public Flux<ServerDto> getAllServers() {
        return http.get()
                   .uri("/bot/api/servers")
                   .retrieve()
                   .bodyToFlux(ServerDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Fetch a single server by its ID.
     *
     * @param serverId the Discord server’s unique identifier
     * @return a {@link Mono} emitting the matching {@link ServerDto}, or empty if 404
     */
    public Mono<ServerDto> getServerById(String serverId) {
        return http.get()
                   .uri("/bot/api/servers/{id}", serverId)
                   .retrieve()
                   .bodyToMono(ServerDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new server record.
     *
     * @param dto the payload describing the new server
     * @return a {@link Mono} emitting the created {@link ServerDto}
     */
    public Mono<ServerDto> createServer(CreateServerRequest dto) {
        return http.post()
                   .uri("/bot/api/servers")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ServerDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update an existing server’s properties.
     *
     * @param serverId the ID of the server to update
     * @param dto      the fields to change
     * @return a {@link Mono} emitting the updated {@link ServerDto}
     */
    public Mono<ServerDto> updateServer(String serverId, UpdateServerRequest dto) {
        return http.put()
                   .uri("/bot/api/servers/{id}", serverId)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ServerDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete a server by ID.
     *
     * @param serverId the ID of the server to remove
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteServer(String serverId) {
        return http.delete()
                   .uri("/bot/api/servers/{id}", serverId)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }

    // ─── Clans ────────────────────────────────────────────────────────────────


    /**
     * Lists all clans that are currently in an active war.
     * 
     * @return a Flux of ClanDto objects representing clans in an active war.
     */
    public Flux<ClanDto> listClansInActiveWar() {
        return http.get()
            .uri("/bot/api/clans/activewar")
            .retrieve()
            .bodyToFlux(ClanDto.class)
            .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }
    
    /**
     * Fetch a clan by its tag.
     *
     * @param clanTag the unique tag of the clan
     * @return a {@link Mono} emitting the matching {@link ClanDto}, or empty if none
     */
    public Mono<ClanDto> getClanById(String clanTag) {
        return http.get()
                   .uri("/bot/api/clans/{tag}", clanTag)
                   .retrieve()
                   .bodyToMono(ClanDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new clan record.
     *
     * @param dto the data needed to create the clan
     * @return a {@link Mono} emitting the created {@link ClanDto}
     */
    public Mono<ClanDto> createClan(CreateClanRequest dto) {
        return http.post()
                   .uri("/bot/api/clans")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ClanDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update the name of an existing clan.
     *
     * @param clanTag the tag of the clan to update
     * @param dto     new name and/or other updatable fields
     * @return a {@link Mono} emitting the updated {@link ClanDto}
     */
    public Mono<ClanDto> updateClan(String clanTag, UpdateClanRequest dto) {
        return http.put()
                   .uri("/bot/api/clans/{tag}", clanTag)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ClanDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete a clan by its tag.
     *
     * @param clanTag the tag of the clan to remove
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteClan(String clanTag) {
        return http.delete()
                   .uri("/bot/api/clans/{tag}", clanTag)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }

    // ─── ClashAccounts ─────────────────────────────────────────────────────────

    /**
     * Fetch a Clash account by tag.
     *
     * @param clashTag the tag of the Clash account
     * @return a {@link Mono} emitting the {@link ClashAccountDto}, or empty if not found
     */
    public Mono<ClashAccountDto> getClashAccount(String clashTag) {
        return http.get()
                   .uri("/bot/api/clash-accounts/{tag}", clashTag)
                   .retrieve()
                   .bodyToMono(ClashAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new Clash account entry.
     *
     * @param dto the details of the new Clash account
     * @return a {@link Mono} emitting the created {@link ClashAccountDto}
     */
    public Mono<ClashAccountDto> createClashAccount(CreateClashAccountRequest dto) {
        return http.post()
                   .uri("/bot/api/clash-accounts")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ClashAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update an existing Clash account.
     *
     * @param clashTag the tag of the Clash account to update
     * @param dto      the updatable fields
     * @return a {@link Mono} emitting the updated {@link ClashAccountDto}
     */
    public Mono<ClashAccountDto> updateClashAccount(String clashTag, UpdateClashAccountRequest dto) {
        return http.put()
                   .uri("/bot/api/clash-accounts/{tag}", clashTag)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ClashAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete a Clash account by tag.
     *
     * @param clashTag the tag of the Clash account to delete
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteClashAccount(String clashTag) {
        return http.delete()
                   .uri("/bot/api/clash-accounts/{tag}", clashTag)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }

    // ─── DiscordAccounts ───────────────────────────────────────────────────────

    /**
     * Fetch a Discord‐linked account by its Discord ID.
     *
     * @param discordId the Discord user’s unique ID
     * @return a {@link Mono} emitting the {@link DiscordAccountDto}, or empty if none
     */
    public Mono<DiscordAccountDto> getDiscordAccount(String discordId) {
        return http.get()
                   .uri("/bot/api/discord-accounts/{id}", discordId)
                   .retrieve()
                   .bodyToMono(DiscordAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new Discord account record.
     *
     * @param dto the details for the new Discord account
     * @return a {@link Mono} emitting the created {@link DiscordAccountDto}
     */
    public Mono<DiscordAccountDto> createDiscordAccount(CreateDiscordAccountRequest dto) {
        return http.post()
                   .uri("/bot/api/discord-accounts")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(DiscordAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update an existing Discord account.
     *
     * @param discordId the ID of the Discord account to update
     * @param dto       the updatable fields
     * @return a {@link Mono} emitting the updated {@link DiscordAccountDto}
     */
    public Mono<DiscordAccountDto> updateDiscordAccount(String discordId, UpdateDiscordAccountRequest dto) {
        return http.put()
                   .uri("/bot/api/discord-accounts/{id}", discordId)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(DiscordAccountDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete a Discord account by its ID.
     *
     * @param discordId the ID of the Discord account to delete
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteDiscordAccount(String discordId) {
        return http.delete()
                   .uri("/bot/api/discord-accounts/{id}", discordId)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }

    // ─── AccountMaps ───────────────────────────────────────────────────────────

    /**
     * Get a mapping between a Discord user and a Clash account.
     *
     * @param discordId the Discord user’s ID
     * @param clashId   the Clash account’s tag
     * @return a {@link Mono} emitting the {@link AccountMapDto}, or empty if none exists
     */
    public Mono<AccountMapDto> getAccountMap(String discordId, String clashId) {
        return http.get()
                   .uri("/bot/api/account-maps/{d}/{c}", discordId, clashId)
                   .retrieve()
                   .bodyToMono(AccountMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * List all Clash‐Discord mappings for a given Discord user.
     *
     * @param discordId the Discord user’s ID
     * @return a {@link Flux} of all {@link AccountMapDto} for that user
     */
    public Flux<AccountMapDto> getAllMappingsForDiscord(String discordId) {
        return http.get()
                   .uri("/bot/api/account-maps/discord/{id}", discordId)
                   .retrieve()
                   .bodyToFlux(AccountMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new mapping between a Discord user and a Clash account.
     *
     * @param dto the association payload
     * @return a {@link Mono} emitting the created {@link AccountMapDto}
     */
    public Mono<AccountMapDto> createAccountMap(CreateAccountMapRequest dto) {
        return http.post()
                   .uri("/bot/api/account-maps")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(AccountMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update an existing mapping by its ID.
     *
     * @param id  the mapping’s primary key
     * @param dto the updatable fields
     * @return a {@link Mono} emitting the updated {@link AccountMapDto}
     */
    public Mono<AccountMapDto> updateAccountMap(Integer id, UpdateAccountMapRequest dto) {
        return http.put()
                   .uri("/bot/api/account-maps/{id}", id)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(AccountMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete an account‐map by its ID.
     *
     * @param id the mapping’s primary key
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteAccountMap(Integer id) {
        return http.delete()
                   .uri("/bot/api/account-maps/{id}", id)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }

    // ─── AccountSettings ─────────────────────────────────────────────────

     /**
     * Update existing account-settings.
     *
     * @param id  the settings record ID
     * @param req the updatable fields
     * @return a {@link Mono} emitting the updated {@link AccountSettingsDto}
     */
    public Mono<AccountSettingsDto> updateAccountSettings(Integer id, UpdateAccountSettingsRequest req) {
        return http.put()
                   .uri("/bot/api/account-settings/{id}", id)
                   .bodyValue(req)
                   .retrieve()
                   .bodyToMono(AccountSettingsDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    // ─── WarNewsSettings ─────────────────────────────────────────────────

    /**
     * Update existing war-news-settings.
     *
     * @param id  the war news settings record ID
     * @param req the updatable fields
     * @return a {@link Mono} emitting the updated {@link WarNewsSettingsDto}
     */
    public Mono<WarNewsSettingsDto> updateWarNewsSettings(Integer id, UpdateWarNewsSettingsRequest req) {
        return http.put()
                   .uri("/bot/api/war-news-settings/{id}", id)
                   .bodyValue(req)
                   .retrieve()
                   .bodyToMono(WarNewsSettingsDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    // ─── ServerSettings ─────────────────────────────────────────────────

    /**
     * Fetch one server-settings by ID.
     *
     * @param id the server-settings primary key
     * @return a {@link Mono} emitting the settings, or empty if not found
     */
    public Mono<ServerSettingsDto> getServerSettings(Integer id) {
        return http.get()
                   .uri("/bot/api/server-settings/{id}", id)
                   .retrieve()
                   .bodyToMono(ServerSettingsDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update existing server-settings.
     *
     * @param id  the settings record ID
     * @param req the updatable fields
     * @return a {@link Mono} emitting the updated {@link ServerSettingsDto}
     */
    public Mono<ServerSettingsDto> updateServerSettings(Integer id, UpdateServerSettingsRequest req) {
        return http.put()
                   .uri("/bot/api/server-settings/{id}", id)
                   .bodyValue(req)
                   .retrieve()
                   .bodyToMono(ServerSettingsDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    // ─── ServerClanMaps ───────────────────────────────────────────────────────

    /**
     * List all server–clan–map associations.
     *
     * @return a {@link Flux} of all {@link ServerDtoClanMapDto} records
     */
    public Flux<ServerClanMapDto> listAllServerClanMaps() {
        return http.get()
                   .uri("/bot/api/server-clan-maps")
                   .retrieve()
                   .bodyToFlux(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * List all server–clan–map associations for a given discord server.
     *
     * @param serverId the Discord server’s ID
     * @return a {@link Flux} of all {@link ServerDtoClanMapDto} records
     */
    public Flux<ServerClanMapDto> listAllServerClanMapsByDiscord(String serverId) {
        return http.get()
                   .uri("/bot/api/server-clan-maps/discord/{s}", serverId)
                   .retrieve()
                   .bodyToFlux(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Fetch the map for a given server & clan combination.
     *
     * @param serverId the Discord server’s ID
     * @param clanTag  the Clash clan’s tag
     * @return a {@link Mono} emitting the {@link ServerDtoClanMapDto}, or empty if none
     */
    public Mono<ServerClanMapDto> getServerClanMapByCombination(String serverId, String clanTag) {
        return http.get()
                   .uri("/bot/api/server-clan-maps/{s}/{c}", serverId, clanTag)
                   .retrieve()
                   .bodyToMono(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Fetch the default map for a given server.
     *
     * @param serverId the Discord server’s ID
     * @return a {@link Mono} emitting the {@link ServerDtoClanMapDto}, or empty if none
     */
    public Mono<ServerClanMapDto> getDefaultServerClanMap(String serverId){
        return http.get()
                   .uri("/bot/api/server-clan-maps/default/{s}", serverId)
                   .retrieve()
                   .bodyToMono(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Create a new server–clan–map.
     *
     * @param dto the mapping payload
     * @return a {@link Mono} emitting the created {@link ServerDtoClanMapDto}
     */
    public Mono<ServerClanMapDto> createServerClanMap(CreateServerClanMapRequest dto) {
        return http.post()
                   .uri("/bot/api/server-clan-maps")
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Update an existing server–clan–map by its ID.
     *
     * @param id  the mapping’s primary key
     * @param dto the updatable fields
     * @return a {@link Mono} emitting the updated {@link ServerDtoClanMapDto}
     */
    public Mono<ServerClanMapDto> updateServerClanMap(Integer id, UpdateServerClanMapRequest dto) {
        return http.put()
                   .uri("/bot/api/server-clan-maps/{id}", id)
                   .bodyValue(dto)
                   .retrieve()
                   .bodyToMono(ServerClanMapDto.class)
                   .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty());
    }

    /**
     * Delete a server–clan–map by its ID.
     *
     * @param id the mapping’s primary key
     * @return a {@link Mono} that completes once deletion succeeds
     */
    public Mono<Void> deleteServerClanMap(Integer id) {
        return http.delete()
                   .uri("/bot/api/server-clan-maps/{id}", id)
                   .retrieve()
                   .toBodilessEntity()
                   .then();
    }
}
