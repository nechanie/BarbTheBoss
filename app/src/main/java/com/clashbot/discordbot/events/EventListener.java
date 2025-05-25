package com.clashbot.discordbot.events;

import reactor.core.publisher.Mono;
import discord4j.core.event.domain.Event;

public interface EventListener<E extends Event> {

    /**  
     * Which Discord4J event type this listener handles.  
     */
    Class<E> getEventType();

    /**
     * Called whenever Discord4J emits an event of type E.
     * @return a Mono that completes when your handling is done.
     */
    Mono<Void> onEvent(E event);
}