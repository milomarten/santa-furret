package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
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
public class EndEventCommand implements SecretSantaCommand {
    private static final ParameterResolver<UUID> EVENT_ID = Parameter.string("event-id")
            .convertLossy(UUID::fromString, "Event IDs are in UUIDv4 format")
            .required();

    private final AdminSecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("end")
                .description("End a Secret Santa event")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("event-id")
                        .description("The Event ID of the event to delete")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .minLength(36).maxLength(36)
                        .build())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var eventId = EVENT_ID.resolve(event);
        var userId = event.getInteraction().getUser().getId();

        var message = Mono.fromCallable(() -> service.endEvent(eventId, userId))
                .thenReturn("The event was deleted.");
        return Responses.delayedEphemeral(message);
    }
}
