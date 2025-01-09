package com.github.milomarten.santa_furret.commands.runner;

import com.github.milomarten.santa_furret.commands.Response;
import com.github.milomarten.santa_furret.commands.Responses;
import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.GiftingNotPermittedException;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BeginGiftingCommand implements SecretSantaCommand {
    private final AdminSecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("begin-gifting")
                .description("Begin the gifting period!")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var userId = IdentityResolver.user().resolve(event).getId();

        var message = Mono.fromCallable(() -> {
            service.beginGifting(guildId, userId);
            return """
                    The gifting period has begun! I have some tools to allow you to help gifting go smoothly.
                    Use `/mark-received <username>` to mark a participant as receiving their gift.
                    Use `/view-unreceived giftee` to view all participants that have not received their gift yet.
                    Conversely, use `/view-unreceived santas` to view all santas that have not sent their gift yet.
                    Finally, you can end the event with `/end`.
                    """;
        })
                .onErrorResume(ex -> {
                    switch (ex) {
                        case EventNotInProgressException ignored -> {
                            return Mono.just("There is currently no event in progress.");
                        }
                        case GiftingNotPermittedException ignored -> {
                            return Mono.just("You can only start gifting an event that is in the Shopping phase");
                        }
                        default -> {
                            return Mono.just(ex.getMessage());
                        }
                    }
                });

        return Responses.delayedEphemeral(message);
    }
}
