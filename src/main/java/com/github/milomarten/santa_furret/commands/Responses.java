package com.github.milomarten.santa_furret.commands;

import discord4j.core.spec.MessageCreateFields;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

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

    public static Response delayedEphemeralWithSpoilerFile(Mono<Tuple2<String, MessageCreateFields.FileSpoiler>> message) {
        return event -> event.deferReply()
                .withEphemeral(true)
                .then(message)
                .flatMap(content -> event.createFollowup(content.getT1())
                        .withFileSpoilers(content.getT2()));
    }
}
