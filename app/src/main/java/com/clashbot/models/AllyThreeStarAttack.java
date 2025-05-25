package com.clashbot.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Ally Three-Star Attack Event Alert Type.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class AllyThreeStarAttack extends Alert {

    /**
     * Represents information about an attack.
     */
    public static record AttackInfo(String attackerName, String defenderTag) {}

    private List<AttackInfo> attacks = new ArrayList<>();

    /**
     * Adds an attack to the list.
     *
     * @param attackerName the name of the attacker
     * @param defenderTag the tag of the defender
     */
    public Mono<Void> addAttack(String attackerName, String defenderTag) {
        return Mono.fromRunnable(() -> attacks.add(new AttackInfo(attackerName, defenderTag)));
    }
    

    /**
     * Gets a list of all recorded attacks.
     *
     * @return the list of attacks
     */
    public List<AttackInfo> getAttacks() {
        return new ArrayList<>(attacks);  // Return a copy to maintain immutability
    }
}