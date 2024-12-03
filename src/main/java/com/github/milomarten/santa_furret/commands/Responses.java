package com.github.milomarten.santa_furret.commands;

import lombok.experimental.UtilityClass;
import reactor.core.publisher.Mono;

@UtilityClass
public class Responses {
    public static Response ephemeral(String message) {
        return event -> event.reply(message).withEphemeral(true);
    }

    public static Response delayedEphemeral(Mono<String> message) {
        return event -> event.deferReply()
                .withEphemeral(true)
                .then(message)
                .flatMap(event::createFollowup);
    }
}
