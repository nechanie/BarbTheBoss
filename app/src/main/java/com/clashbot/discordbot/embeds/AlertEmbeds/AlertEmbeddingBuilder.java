package com.clashbot.discordbot.embeds.AlertEmbeds;

import com.clashbot.models.Alert;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public interface AlertEmbeddingBuilder<T extends Alert> {


    Mono<EmbedCreateSpec> materialize();

}
