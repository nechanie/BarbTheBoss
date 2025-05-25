package com.clashbot.models;

import java.util.ArrayList;
import java.util.List;

import com.clashbotbackend.dto.DiscordAccountDto;
import com.lycoon.clashapi.models.war.WarMember;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Attack Reminder Alert Type.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class AttackReminder extends Alert {
    /**
     * Represents information about a clan war member.
     */
    public static record MemberInfo(WarMember member,  DiscordAccountDto discordAccount) {}

    private List<MemberInfo> members = new ArrayList<>();

    private int attacksPerMember;

    /**
     * Adds a member to the list.
     *
     * @param member a {@link WarMember} object representing the war member with attacks left in the war.
     * @param discordAccount a {@link DiscordAccountDto} tied to the WarMember account for discord mention integration. {@link null} allowed for missing connections.
     */
    public Mono<Void> addMember(WarMember member, DiscordAccountDto discordAccount) {
        return Mono.fromRunnable(() -> members.add(new MemberInfo(member, discordAccount)));
    }
    

    /**
     * Gets a list of all recorded members.
     *
     * @return {@link List}<{@link MemberInfo}> the list of members
     */
    public List<MemberInfo> getMembers() {
        return new ArrayList<>(members);  // Return a copy to maintain immutability
    }
}
