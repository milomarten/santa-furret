package com.github.milomarten.santa_furret.commands.registration;

import com.github.milomarten.santa_furret.commands.ContextualSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UpdateCommand extends ContextualSecretSantaCommand {
    private static final ParameterResolver<Boolean> RECEIVING_NSFW = Parameter.bool("receiving-nsfw")
            .required();
    private static final ParameterResolver<Boolean> GIVING_NSFW = Parameter.bool("giving-nsfw")
            .required();

    private final SecretSantaParticipantRepository repository;

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

//    @Override
//    public Response handleCommand(ChatInputInteractionEvent event) {
//        var guildId = GuildIdResolver.required().resolve(event);
//        var userId = event.getInteraction().getUser().getId();
//
//        var options = new ParticipantOptions(
//                RECEIVING_NSFW.resolve(event),
//                GIVING_NSFW.resolve(event)
//        );
//
//        var message = Mono.fromCallable(() -> service.updateParticipant(guildId, userId, options))
//                .thenReturn("I've updated your preferences! Thanks!")
//                .onErrorResume(ex -> {
//                    if (ex instanceof EventNotInProgressException) {
//                        return Mono.just("There isn't currently an event in progress.");
//                    } else if (ex instanceof RegistrationNotPermittedException) {
//                        return Mono.just("You aren't registered for this event. Use `/register` to join!");
//                    } else {
//                        return Mono.just(ex.getMessage());
//                    }
//                });
//        return Responses.delayedEphemeral(message);
//    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION, EventStatus.SHOPPING);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var userId = IdentityResolver.user().resolve(cmd).getId();

        var userMaybe = repository.getByEventIdAndParticipantId(event.getId(), userId.asLong());
        if (userMaybe.isEmpty()) {
            return "You must first register for this event with `/register`";
        }

        var entry = userMaybe.get();
        entry.setOkReceivingNsfw(RECEIVING_NSFW.resolve(cmd));
        entry.setOkGivingNsfw(GIVING_NSFW.resolve(cmd));
        repository.save(entry);

        if (event.getStatus() == EventStatus.REGISTRATION) {
            return "I've updated your preferences! Thanks!";
        } else {
            return "I've updated your preferences! Since the event is already underway, you may want to alert your santa of the change.";
        }
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED) {
            return "The event hasn't been kicked off yet! Please wait until the event runner is ready.";
        } else {
            return "The event is already mostly completed, so updating your preferences won't mean much.";
        }
    }
}
