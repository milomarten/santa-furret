package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateEventCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("create")
                .description("Create a secret santa event!")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public Mono<?> handleCommand(ChatInputInteractionEvent event) {
        return null;
    }
}
