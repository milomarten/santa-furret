package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.StringJoiner;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class EntrantsCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("entrants")
                .description("View all entrants in the current Secret Santa")
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);

        var response = Mono.fromCallable(() -> service.getParticipants(guildId))
                .flatMapIterable(Function.identity())
                .flatMap(participant -> event.getClient()
                        .getUserById(Snowflake.of(participant.getParticipantId()))
                        .map(User::getUsername)
                )
                .collectList()
                .map(list -> {
                    if (list.isEmpty()) {
                        return "There are no participants in the event yet.";
                    }
                    var sj = new StringJoiner(
                            ", ",
                            "There are %d participant(s) in the event so far:\n".formatted(list.size()), "");
                    list.forEach(sj::add);
                    return sj.toString();
                });
        return Responses.delayedEphemeral(response);
    }
}
