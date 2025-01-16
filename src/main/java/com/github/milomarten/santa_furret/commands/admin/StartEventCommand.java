package com.github.milomarten.santa_furret.commands.admin;

import com.github.milomarten.santa_furret.commands.ContextualAdminSecretSantaCommand;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class StartEventCommand extends ContextualAdminSecretSantaCommand {
    private final SecretSantaEventRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("start")
                .description("Start an unstarted Secret Santa event.")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.NOT_STARTED);
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
       event.setStatus(EventStatus.REGISTRATION);
       repository.save(event);
        return """
            Your event has started! Participants can use `/register` to join. Remember, I'm a quiet \
            bot, so now is the time to make your announcement.
            When you want to end registration and assign santas, use `/assign-santas`.
        """;
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        return "This command can only be used if an event is created but not yet started";
    }
}
