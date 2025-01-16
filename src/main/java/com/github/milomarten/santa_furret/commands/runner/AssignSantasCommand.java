package com.github.milomarten.santa_furret.commands.runner;

import com.github.milomarten.santa_furret.commands.ContextualAdminSecretSantaCommand;
import com.github.milomarten.santa_furret.matchup.GenerateMatchupsService;
import com.github.milomarten.santa_furret.matchup.NotEnoughParticipantsException;
import com.github.milomarten.santa_furret.matchup.UnableToAssignGiftee;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssignSantasCommand extends ContextualAdminSecretSantaCommand {
    private final AdminSecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("assign-santas")
                .description("End the registration period and draw santas")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        try {
            var matchups = service.generateMatchups(event.getId());

            return """
                    Matchups have been made! There are %d total entrants. I'll be sending each Santa a DM \
                    over the next few moments with their giftee. How exciting!
                    For you, here are the commands for the next steps:
                    Use `/begin-gifting` to indicate that users should begin gifting their presents! It is up to you to tell them where to do so.
            """.formatted(matchups.size());
        } catch (NotEnoughParticipantsException nepx) {
            return "There is not enough participants in the event. The minimum is %d."
                    .formatted(GenerateMatchupsService.MINIMUM_PARTICIPANTS);
        } catch (UnableToAssignGiftee utag) {
            return """
                I wasn't able to generate matchups, due to the interactions between blacklists. \
                This is a rare situation, and may require additional participants to resolve. I recommend \
                looking into the participant list for more information.
            """;
        }
    }

    @Override
    protected Set<EventStatus> expectedStatuses() {
        return EnumSet.of(EventStatus.REGISTRATION);
    }

    @Override
    protected String unexpectedStatusMessage(EventStatus actual) {
        if (actual == EventStatus.NOT_STARTED) {
            return "You can't draw names until the event is started.";
        } else {
            return "The names have already been drawn for this event.";
        }
    }
}
