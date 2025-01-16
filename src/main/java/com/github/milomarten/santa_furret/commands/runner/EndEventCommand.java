package com.github.milomarten.santa_furret.commands.runner;

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
public class EndEventCommand extends ContextualAdminSecretSantaCommand {
    private final SecretSantaEventRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("end")
                .description("End a Secret Santa event")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        event.setStatus(EventStatus.ENDED);
        repository.save(event);
        return "The event has been ended. Thanks for having me!";
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.GIFTING);
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        return "The event has already ended.";
    }
}
