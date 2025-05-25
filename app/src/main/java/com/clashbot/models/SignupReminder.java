package com.clashbot.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Signup Reminder Alert Type.
 */
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class SignupReminder extends Alert {
    private String clanName;
}

