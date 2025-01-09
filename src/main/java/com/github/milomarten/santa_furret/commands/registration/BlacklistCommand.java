package com.github.milomarten.santa_furret.commands.registration;

import com.github.milomarten.santa_furret.commands.ContextualSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
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
public class BlacklistCommand extends ContextualSecretSantaCommand {
    private static final ParameterResolver<Snowflake> USER = Parameter.snowflake("user")
            .required();

    private final SecretSantaParticipantRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("blacklist")
                .description("Mark a user that you are not comfortable being matched with.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("The user you want to blacklist.")
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var userId = IdentityResolver.user().resolve(cmd).getId();
        var blockId = USER.resolve(cmd);

        var userMaybe = repository.getByEventIdAndParticipantId(event.getId(), userId.asLong());
        if (userMaybe.isEmpty()) {
            return "You must first register for this event with `/register`";
        }
        var entry = userMaybe.get();
        entry.getBlacklist().add(blockId.asLong());
        repository.save(entry);

        return "Sure, I blacklisted that user for you.";
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED) {
            return "The event hasn't been kicked off yet! Please wait until the event runner is ready.";
        } else {
            return "The matchups have already been chosen for this event. Talk with the event owner for help.";
        }
    }
}
