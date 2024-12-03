package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.models.exception.NoSuchEvent;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StartEventCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    private static final ParameterResolver<UUID> EVENT_ID = Parameter.string("event-id")
            .convertLossy(UUID::fromString, "Event ID is in UUIDv4 format")
            .required();

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("start")
                .description("Start an unstarted Secret Santa event.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("event-id")
                        .description("The Event ID to start")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .minLength(36).maxLength(36)
                        .build())
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var eventRunner = event.getInteraction().getUser().getId();
        var eventId = EVENT_ID.resolve(event);

        Mono<String> message = Mono.fromCallable(() -> service.startEvent(eventId, eventRunner))
                .map(ss -> {
                    return """
                            Your event has started! Participants can use `/register` to join. Remember, I'm a quiet \
                            bot, so you have to announce that the event has begun!
                            When you want to draw names, use `/draw %s` to continue.
                            """.formatted(ss.getId());
                })
                .onErrorResume(e -> {
                    if (e instanceof EventInProgressException) { return Mono.just("This event is already in progress."); }
                    else if (e instanceof NoSuchEvent) { return Mono.just("I don't know any event with that ID."); }
                    else { return Mono.just(e.getMessage()); }
                });
        return Responses.delayedEphemeral(message);
    }
}
