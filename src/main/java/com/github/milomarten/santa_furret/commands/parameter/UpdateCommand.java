package com.github.milomarten.santa_furret.commands.parameter;

import com.github.milomarten.santa_furret.commands.Response;
import com.github.milomarten.santa_furret.commands.Responses;
import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.models.ParticipantOptions;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.RegistrationNotPermittedException;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UpdateCommand implements SecretSantaCommand {
    private static final ParameterResolver<Boolean> RECEIVING_NSFW = Parameter.bool("receiving-nsfw")
            .required();
    private static final ParameterResolver<Boolean> GIVING_NSFW = Parameter.bool("giving-nsfw")
            .required();

    private final SecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("update")
                .description("Update your preferences for the current Secret Santa event.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("receiving-nsfw")
                        .description("Indicate that you are OK receiving NSFW")
                        .required(true)
                        .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("giving-nsfw")
                        .description("Indicate that you are OK giving NSFW")
                        .required(true)
                        .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                        .build())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = event.getInteraction().getUser().getId();

        var options = new ParticipantOptions(
                RECEIVING_NSFW.resolve(event),
                GIVING_NSFW.resolve(event)
        );

        var message = Mono.fromCallable(() -> service.updateParticipant(guildId, userId, options))
                .thenReturn("I've updated your preferences! Thanks!")
                .onErrorResume(ex -> {
                    if (ex instanceof EventNotInProgressException) {
                        return Mono.just("There isn't currently an event in progress.");
                    } else if (ex instanceof RegistrationNotPermittedException) {
                        return Mono.just("You aren't registered for this event. Use `/register` to join!");
                    } else {
                        return Mono.just(ex.getMessage());
                    }
                });
        return Responses.delayedEphemeral(message);
    }
}
