package com.clashbot.discordbot.embeds.CommandEmbeds;

import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public interface SlashCommandEmbed<T> {

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

    T setHelp();
}
