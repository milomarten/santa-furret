package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.ParticipantNotRegisteredException;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BlacklistCommand implements SecretSantaCommand {
    private static final ParameterResolver<Snowflake> USER = Parameter.snowflake("user")
            .required();

    private final SecretSantaService service;

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
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guild = GuildIdResolver.required().resolve(event);
        var userId = event.getInteraction().getUser().getId();
        var blockId = USER.resolve(event);

        Mono<String> message = Mono.fromCallable(() -> service.addBlacklist(guild, userId, blockId))
                .map(ssp -> "Sure, I blacklisted that user for you.")
                .onErrorResume(ex -> switch (ex) {
                    case EventNotInProgressException ignored ->
                            Mono.just("There isn't currently an event in progress.");
                    case ParticipantNotRegisteredException ignored ->
                            Mono.just("You aren't currently registered for the event! You need to register first.");
                    case EventInProgressException ignored ->
                            Mono.just("The matchups have already been chosen. Talk with the event owner for help.");
                    default -> Mono.just(ex.getMessage());
                });
        return Responses.delayedEphemeral(message);
    }
}