package com.clashbot.models;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Perfect War Event Alert Type.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class PerfectWar extends Alert {
    private String clanName;
    private String opponentName;
}