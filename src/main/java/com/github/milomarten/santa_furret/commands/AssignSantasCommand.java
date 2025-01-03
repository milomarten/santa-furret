package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.matchup.NotEnoughParticipantsException;
import com.github.milomarten.santa_furret.matchup.UnableToAssignGiftee;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.MatchupNotPermittedException;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AssignSantasCommand implements SecretSantaCommand {
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
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = IdentityResolver.user().resolve(event).getId();

        var message = Mono.fromCallable(() -> {
            var matchups = service.generateMatchups(guildId, userId);
            // todo: DM everyone!
            return """
                    Matchups have been made! There are %d total entrants. I'll be sending each Santa a DM \
                    over the next few moments with their giftee. How exciting!
                    """.formatted(matchups.size());
        })
                .onErrorResume(ex -> {
                    switch (ex) {
                        case EventNotInProgressException ignored -> {
                            return Mono.just("There is currently no event in progress.");
                        }
                        case MatchupNotPermittedException ignored -> {
                            return Mono.just("You can only draw names for an event currently in registration state.");
                        }
                        case NotEnoughParticipantsException ignored -> {
                            return Mono.just("There is not enough participants in the event. The minimum is 5.");
                        }
                        case UnableToAssignGiftee u -> {
                            if (u.getSanta() == null) {
                                return Mono.just("""
                                        I wasn't able to generate matchups, due to the interactions between multiple blacklists. \
                                        This is a rare situation, and may require additional participants to resolve. I recommend \
                                        looking into the participant list for more information.
                                        """);
                            } else {
                                return event.getClient()
                                        .getUserById(Snowflake.of(u.getSanta().getParticipantId()))
                                        .map(User::getUsername)
                                        .map("""
                                                I wasn't able to generate matchups, because I had difficulty assigning user \
                                                %s to any other participants. This is a rare situation, and may require additional \
                                                participants to resolve. You can look at the participant list for more information.
                                                """::formatted);
                            }
                        }
                        default -> {
                            return Mono.just(ex.getMessage());
                        }
                    }
                });
        return Responses.delayedEphemeral(message);
    }
}
