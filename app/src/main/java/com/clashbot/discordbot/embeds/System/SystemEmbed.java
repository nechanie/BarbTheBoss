package com.clashbot.discordbot.embeds.System;

import discord4j.core.event.domain.Event;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public interface SystemEmbed<T, E extends Event> {
    /**
     * A generic factory that uses reflection.
     * Caller must pass in the Class object of the type they want.
     */
    static <T> T init(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    EmbedCreateSpec materialize();

    Mono<EmbedCreateSpec> materializeMono();

    T setViewContent(E event);

}
