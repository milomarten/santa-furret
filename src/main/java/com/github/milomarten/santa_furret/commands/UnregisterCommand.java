package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.RegistrationNotPermittedException;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UnregisterCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("unregister")
                .description("Leave the current Secret Santa event.")
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = event.getInteraction().getUser().getId();

        var message = Mono.fromCallable(() -> service.removeParticipant(guildId, userId))
                .map(deleted -> {
                    return deleted ? "I've unregistered you. Sorry to see you go!" : "You weren't registered for the event!";
                })
                .onErrorResume(ex -> {
                    if (ex instanceof EventNotInProgressException) {
                        return Mono.just("There isn't currently an event in progress.");
                    } else if (ex instanceof RegistrationNotPermittedException) {
                        return Mono.just("The registration window for this event is closed. If you need to back out, contact the event runner!");
                    } else {
                        return Mono.just(ex.getMessage());
                    }
                });
        return Responses.delayedEphemeral(message);
    }
}
