package com.github.milomarten.santa_furret.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

public interface SecretSantaCommand {
    ApplicationCommandRequest getSpec();
    Response handleCommand(ChatInputInteractionEvent event);

    SecretSantaCommand NOOP = new SecretSantaCommand() {
        @Override
        public ApplicationCommandRequest getSpec() {
            return null;
        }

        @Override
        public Response handleCommand(ChatInputInteractionEvent event) {
            return Response.NOOP;
        }
    };
}
