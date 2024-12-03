package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.Parameter;
import com.github.milomarten.santa_furret.commands.parameter.ParameterResolver;
import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import com.github.milomarten.santa_furret.models.ParticipantOptions;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.RegistrationNotPermittedException;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RegisterCommand implements SecretSantaCommand {
    private final SecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("register")
                .description("Register for the current Secret Santa event.")
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = event.getInteraction().getUser().getId();

        var message = Mono.fromCallable(() -> service.addParticipant(guildId, userId))
                .zipWith(event.getInteraction().getGuild())
                .map(tuple -> {
                    var sse = tuple.getT1().getEvent();
                    return """
                            Hey! Thank you for participating in %s's Secret Santa event. Here's what will happen next:
                            - When the event runner is ready, they will kickoff the name draw. I'll let you know your giftee personally.
                            - You'll be responsible for getting your giftee a present. Use `/wishlist giftee` for help!
                            - When the event runner says so, they will close the gifting period, and you'll reveal yourself and your gift!
                            And what you can do until then:
                            - You can use `/update` to update your NSFW preferences at any time.
                            - You can use `/unregister` to back out before the registration deadline, no sweat. If you need \
                            to back out after that, contact the event organizer.
                            - You can use `/blacklist` during registration to indicate that you aren't comfortable being matched up with someone. \
                            I'll make sure they aren't your gifter or your giftee.
                            Thanks again!
                            """.formatted(tuple.getT2().getName());
                })
                .onErrorResume(ex -> {
                    return switch (ex) {
                        case EventNotInProgressException e ->
                                Mono.just("There isn't currently an event in progress.");
                        case RegistrationNotPermittedException e ->
                                Mono.just("The registration window for this event is closed. Sorry!");
                        case DataIntegrityViolationException e ->
                                Mono.just("You're already registered for this event!");
                        default -> Mono.just(ex.getMessage());
                    };
                });
        return Responses.delayedEphemeral(message);
    }
}
