package com.github.milomarten.santa_furret.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface Response {
    Mono<?> respond(ChatInputInteractionEvent event);

    Response NOOP = event -> Mono.empty();
}
