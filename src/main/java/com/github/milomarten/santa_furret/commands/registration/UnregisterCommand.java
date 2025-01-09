package com.github.milomarten.santa_furret.commands.registration;

import com.github.milomarten.santa_furret.commands.ContextualSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UnregisterCommand extends ContextualSecretSantaCommand {
    private final SecretSantaParticipantRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("unregister")
                .description("Leave the current Secret Santa event.")
                .build();
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var userId = IdentityResolver.user().resolve(cmd).getId();

        var participantMaybe = repository.getByEventIdAndParticipantId(event.getId(), userId.asLong());
        if (participantMaybe.isPresent()) {
            repository.delete(participantMaybe.get());
            return "I've unregistered you. Sorry to see you go!";
        } else {
            return "You weren't registered for the event!";
        }
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED) {
            return "The event hasn't been kicked off yet! Please wait until the event runner is ready.";
        } else {
            return "The registration window has already closed unfortunately. Contact the Event Runner if you really need to drop out.";
        }
    }
}
