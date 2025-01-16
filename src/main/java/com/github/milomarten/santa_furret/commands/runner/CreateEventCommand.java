package com.github.milomarten.santa_furret.commands.runner;

import com.github.milomarten.santa_furret.commands.Response;
import com.github.milomarten.santa_furret.commands.Responses;
import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import com.github.milomarten.santa_furret.util.Permission;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CreateEventCommand implements SecretSantaCommand {
    private final SecretSantaService service;
    private final SecretSantaEventRepository repository;

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
        var eventRunner = IdentityResolver.user().resolve(event);

        Mono<String> message = Mono.fromCallable(() -> {
            var eventMaybe = service.getCurrentEventFor(guildId);
            if (eventMaybe.isPresent()) {
                return "Sorry, an event is already in progress! For convenience, I can only hold one at a time per server.";
            }

            var newEvent = new SecretSantaEvent();
            newEvent.setOrganizer(eventRunner.getId().asLong());
            newEvent.setHomeGuild(guildId.asLong());
            newEvent.setStatus(EventStatus.NOT_STARTED);
            newEvent = repository.save(newEvent);

            return  """
                Your event has been created! The ID is %s, don't forget it!
                I'm a quiet bot, so it's your responsibility to tell your server about the event. I'm \
                also not automatic, so it's your responsibility to use `/start` when you're ready for registrants. \
                I'll give you more details once the event starts!
                """.formatted(newEvent.getId());
        });
        return Responses.delayedEphemeral(message);
    }
}
