package com.github.milomarten.santa_furret.commands.registration;

import com.github.milomarten.santa_furret.commands.ContextualSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterCommand extends ContextualSecretSantaCommand {
    private final SecretSantaParticipantRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("register")
                .description("Register for the current Secret Santa event.")
                .build();
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var userId = IdentityResolver.user().resolve(cmd).getId();
        var participant = repository.save(new SecretSantaParticipant(
                null,
                event,
                userId.asLong(),
                false,
                false,
                Set.of()
        ));
        log.info("Created participant {} - Name: {}, Event: {}",
                participant.getId(),
                cmd.getInteraction().getUser().getUsername(),
                event.getId());
        return """
            Hey! Thank you for participating! Here's what will happen next:
            - When the event runner is ready, the owner will kickoff the name draw. I'll let you know your giftee personally.
            - On the event day, you'll reveal yourself to your giftee with their present.
            And what you can do until then:
            - You can use `/update` to update your NSFW preferences at any time.
            - You can use `/unregister` to back out before the registration deadline, no sweat. If you need \
            to back out after that, contact the event organizer.
            - You can use `/blacklist` during registration to indicate that you aren't comfortable being matched up with someone. \
            I'll make sure they aren't your gifter or your giftee.
            Thanks again!
            """;
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        return switch (actual) {
            case NOT_STARTED -> "The event hasn't been kicked off yet! Please wait until the event runner is ready.";
            default -> "The registration window has already closed unfortunately. Try again next time.";
        };
    }
}
