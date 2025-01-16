package com.github.milomarten.santa_furret.commands.runner;

import com.github.milomarten.santa_furret.commands.ContextualAdminSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteEventCommand extends ContextualAdminSecretSantaCommand {
    private static final ParameterResolver<UUID> EVENT_ID = Parameter.string("event-id")
            .convertLossy(UUID::fromString, "Event IDs are in UUIDv4 format")
            .required();

    private final SecretSantaEventRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("delete")
                .description("Delete a Secret Santa event")
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
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var eventId = EVENT_ID.resolve(cmd);
        if (Objects.equals(event.getId(), eventId)) {
            repository.deleteById(eventId);
            return "The event was deleted.";
        } else {
            return "The event ID provided was incorrect. Please check and try again!";
        }
    }
}
