package com.clashbot.models;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * Lead Change Event Alert Type.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class LeadChange extends Alert {
    private String clanName;
    private String opponentName;
    private int clanStars;
    private int opponentStars;
    private boolean isClanLeading;
}
