package com.clashbot.service.utils;

import org.springframework.stereotype.Service;

import com.clashbot.database.ClashBotApiClient;
import com.clashbotbackend.dto.ServerClanMapDto;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

@Service
public class PermissionCheck {
    
    private Snowflake adminOverride = Snowflake.of("213523881003057153");

    private final ClashBotApiClient api;

    public PermissionCheck(ClashBotApiClient api){
        this.api = api;
    }

    public Mono<Boolean> checkServerClanPermission(Mono<Member> memberMono, ServerClanMapDto map) {

        Mono<Member> cachedMember = memberMono.cache();
        return cachedMember.flatMap(member ->{
            if (member.getId().equals(adminOverride)){
                return Mono.just(true);
            }
            return member.getBasePermissions()
                .map(perms -> perms.contains(Permission.MANAGE_GUILD)); 
        }).filter(Boolean::booleanValue)
        .switchIfEmpty(
            Mono.defer(() ->
                cachedMember.flatMap(member ->
                    api.getAllMappingsForDiscord(member.getId().asString())
                    .filter(mapping -> mapping.clashAccount().clan().clanId().equalsIgnoreCase(map.clan().clanId()) && mapping.clashAccount().isLeader())
                    .singleOrEmpty()
                )
            ).hasElement()
        );
    }

    public Mono<Boolean> checkServerPermission(Mono<Member> memberMono){
        Mono<Member> cachedMember = memberMono.cache();

        return cachedMember.flatMap(member ->{
            if (member.getId().equals(adminOverride)){
                return Mono.just(true);
            }
            return member.getBasePermissions()
                .map(perms -> perms.contains(Permission.MANAGE_GUILD)); 
        }).filter(Boolean::booleanValue);
    }
}
