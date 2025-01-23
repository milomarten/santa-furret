package com.github.milomarten.santa_furret.commands.admin;

import com.github.milomarten.santa_furret.commands.ContextualAdminSecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.ObjectParameter;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BlacklistCommand extends ContextualAdminSecretSantaCommand {
    private final SecretSantaParticipantRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("blacklist")
                .description("Mark a user that you are not comfortable being matched with.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("The user whose blacklist you want to modify.")
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("target")
                        .description("The user to add to the blacklist.")
                        .type(ApplicationCommandOption.Type.USER.getValue())
                        .required(true)
                        .build())
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var parameters = Parameter.PARSER.resolve(cmd);

        var userMaybe = repository.getByEventIdAndParticipantId(event.getId(), parameters.userId.asLong());
        if (userMaybe.isEmpty()) {
            return "You must first register for this event with `/register`";
        }
        var entry = userMaybe.get();
        entry.getBlacklist().add(parameters.targetId.asLong());
        repository.save(entry);

        return "Sure, blacklisting complete.";
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED) {
            return "The event hasn't been kicked off yet! Please wait until the event runner is ready.";
        } else {
            return "The matchups have already been chosen for this event. Talk with the event owner for help.";
        }
    }

    @Data
    public static class Parameter {
        private Snowflake userId;
        private Snowflake targetId;

        public static final ObjectParameter<Parameter> PARSER =
                new ObjectParameter<>(Parameter::new)
                        .asSnowflake("user", Parameter::setUserId)
                        .asSnowflake("target", Parameter::setTargetId);
    }
}
