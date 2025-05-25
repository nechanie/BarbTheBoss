package com.clashbot.models;

import java.util.List;
import java.util.Map;

import com.clashbotbackend.dto.ServerClanMapDto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for all alert types to ensure consistency and structure.
 */
@Data
@NoArgsConstructor
public abstract class Alert {
    private List<ServerClanMapDto> serverClans;
    private Map<String, Object> additionalData;
}
