package com.github.milomarten.santa_furret.commands.runner;

import com.github.milomarten.santa_furret.commands.Response;
import com.github.milomarten.santa_furret.commands.Responses;
import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.service.AdminSecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateEventCommand implements SecretSantaCommand {
    private final AdminSecretSantaService service;

    @Override
    public ApplicationCommandRequest getSpec() {
        return ApplicationCommandRequest.builder()
                .name("create")
                .description("Create a Secret Santa event!")
                .defaultMemberPermissions(Permission.MANAGE_GUILD.single())
                .build();
    }

    @Override
    public Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);
        var eventRunner = event.getInteraction().getUser().getId();

        Mono<String> message = Mono.fromCallable(() -> service.createEvent(guildId, eventRunner))
                .map(ss -> """
                        Your event has been created! The ID is %s, don't forget it!
                        I'm a quiet bot, so it's your responsibility to tell your server about the event. I'm \
                        also not automatic, so it's your responsibility to use `/advance %s` when you're ready. I'll \
                        give you more details once the event starts!
                        """.formatted(ss.getId(), ss.getId()))
                .onErrorResume(e -> {
                    if (e instanceof EventInProgressException) { return Mono.just("An event is already in progress for this server. I can't conduct two at once."); }
                    else { return Mono.just(e.getMessage()); }
                });
        return Responses.delayedEphemeral(message);
    }
}
