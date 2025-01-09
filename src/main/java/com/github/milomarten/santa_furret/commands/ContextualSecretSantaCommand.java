package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.GuildIdResolver;
import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
public abstract class ContextualSecretSantaCommand implements SecretSantaCommand {
    protected SecretSantaService service;

    @Autowired
    public void setService(SecretSantaService service) {
        this.service = service;
    }

    @Override
    public final Response handleCommand(ChatInputInteractionEvent event) {
        var guildId = GuildIdResolver.required().resolve(event);

        var message = Mono.fromCallable(() -> {
            return service.getCurrentEventFor(guildId)
                    .orElse(null);
        })
                .flatMap(sse -> {
                    if (expectedStatuses().contains(sse.getStatus())) {
                        try {
                            return Mono.just(handleCommand(sse, event));
                        } catch (Exception ex) {
                            return Mono.error(ex);
                        }
                    } else {
                        return Mono.just(unexpectedStatusMessage(sse.getStatus()));
                    }
                })
                .onErrorResume(ex -> {
                    log.error("Encountered error executing command", ex);
                    if (ex instanceof ParameterValidationFailure pvf) {
                        return Mono.error(pvf); // Propagate
                    }
                    return Mono.just("I encountered an error executing your request. Please try again, or contact the event runner.");
                })
                .defaultIfEmpty("There is currently no event in progress.");
        return Responses.delayedEphemeral(message);
    }

    public abstract String handleCommand(SecretSantaEvent event, ChatInputInteractionEvent cmd);

    protected Set<EventStatus> expectedStatuses() { return EnumSet.allOf(EventStatus.class); }

    protected String unexpectedStatusMessage(EventStatus actual) {
        return "Now isn't the time to use that command!";
    }
}
