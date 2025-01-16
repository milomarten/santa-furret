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
public class BeginGiftingCommand extends ContextualAdminSecretSantaCommand {
    private final SecretSantaEventRepository repository;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("begin-gifting")
                .description("Begin the gifting period!")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        event.setStatus(EventStatus.GIFTING);
        repository.save(event);
        return """
            The gifting period has begun! I have some tools to allow you to help gifting go smoothly.
            Use `/mark-received <username>` to mark a participant as receiving their gift.
            Use `/view-unreceived giftee` to view all participants that have not received their gift yet.
            Conversely, use `/view-unreceived santas` to view all santas that have not sent their gift yet.
            Finally, you can end the event with `/end`.
        """;
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.SHOPPING);
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED || actual == EventStatus.REGISTRATION) {
            return "Can't begin gifting until matchups are made.";
        } else {
            return "Gifting has already been started";
        }
    }
}
