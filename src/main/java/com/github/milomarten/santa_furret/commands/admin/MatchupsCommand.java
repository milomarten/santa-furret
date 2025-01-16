package com.github.milomarten.santa_furret.commands.admin;

import com.github.milomarten.santa_furret.commands.Response;
import com.github.milomarten.santa_furret.commands.Responses;
import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
import com.github.milomarten.santa_furret.service.CachedUsernameService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class MatchupsCommand implements SecretSantaCommand {
    private final AdminSecretSantaService service;
    private final CachedUsernameService usernameService;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("matchups")
                .description("Receive a document of all the matchups for the current event.")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = IdentityResolver.user().resolve(event).getId();

        var results = Mono.fromCallable(() -> service.getMatchups(guildId, userId))
                .flatMapIterable(Function.identity())
                .flatMap(matchup -> {
                    return Mono.zip(
                            usernameService.getUsername(Snowflake.of(matchup.getSanta().getParticipantId())),
                            usernameService.getUsername(Snowflake.of(matchup.getGiftee().getParticipantId())),
                            (santa, giftee) -> santa + " gifting to " + giftee);
                })
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "There are no matchups for this event yet.";
                    } else {
                        return String.join("\n", list);
                    }
                })
                .onErrorResume(ex -> {
                    return Mono.justOrEmpty(ex.getMessage());
                })
                .map(fileContents -> Tuples.of(
                        "Find attached a list of matchups. Look at your peril!",
                        MessageCreateFields.FileSpoiler.of(
                                "secret-santa-results.txt",
                                new ByteArrayInputStream(fileContents.getBytes())
                        ))
                );

        return Responses.delayedEphemeralWithSpoilerFile(results);
    }
}
