package com.github.milomarten.santa_furret.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface SecretSantaCommand {
    ApplicationCommandRequest getSpec();
    Mono<?> handleCommand(ChatInputInteractionEvent event);

    SecretSantaCommand NOOP = new SecretSantaCommand() {
        @Override
        public ApplicationCommandRequest getSpec() {
            return null;
        }

        @Override
        public Mono<?> handleCommand(ChatInputInteractionEvent event) {
            return Mono.empty();
        }
    };
}
