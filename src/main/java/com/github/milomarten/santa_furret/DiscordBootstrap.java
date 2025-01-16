package com.github.milomarten.santa_furret;

import com.github.milomarten.santa_furret.commands.SecretSantaCommand;
import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import com.github.milomarten.santa_furret.service.CachedUsernameService;
import com.github.milomarten.santa_furret.service.diff.CommandDiffService;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordBootstrap implements ApplicationListener<ApplicationReadyEvent> {
    public static final Snowflake MILO_ID = Snowflake.of(248612704019808258L);

    private final DiscordClient client;
    private final List<SecretSantaCommand> commands;
    private final CachedUsernameService cachedUsernameService;
    private final CommandDiffService diffService;

    private final Map<String, SecretSantaCommand> commandMap = new HashMap<>();
    private final List<ApplicationCommandRequest> requests = new ArrayList<>();

    @PostConstruct
    public void setUp() {
        for (var command : commands) {
            var spec = command.getSpec();
            this.requests.add(spec);
            this.commandMap.put(spec.name(), command);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent _e) {
        client.withGateway(gateway -> {
            cachedUsernameService.setClient(gateway);
            var chatInputHook = gateway
                    .on(ChatInputInteractionEvent.class, event -> Mono.defer(() -> commandMap
                            .getOrDefault(event.getCommandName(), SecretSantaCommand.NOOP)
                            .handleCommand(event)
                            .respond(event))
                            .then()
                            .onErrorResume(ParameterValidationFailure.class, ex ->
                                    event.reply(ex.getMessage()).withEphemeral(true)
                            )
                    )
                    .onErrorContinue((ex, obj) -> log.error("Error with Chat Input Hook: {}", obj, ex));
            var textHook = gateway
                    .on(MessageCreateEvent.class, event -> {
                        var content = event.getMessage().getContent();
                        if (isMe(event) && !content.isEmpty() && content.charAt(0) == '!') {
                            var appService = gateway.getRestClient().getApplicationService();
                            var appId = gateway.getRestClient().getApplicationId();
                            if (content.equals("!local")) {
                                return runDiffOverwriteCommand(appId,
                                        id -> appService.getGuildApplicationCommands(id, 423976318082744321L),
                                        id -> appService.bulkOverwriteGuildApplicationCommand(id, 423976318082744321L, this.requests),
                                        (id, cid) -> appService.deleteGuildApplicationCommand(id, 423976318082744321L, cid)
                                )
                                .flatMap(t -> event.getMessage().getChannel().flatMap(mc -> mc.createMessage(t.toString())));
                            } else if (content.equals("!global")) {
                                return runDiffOverwriteCommand(appId,
                                        appService::getGlobalApplicationCommands,
                                        id -> appService.bulkOverwriteGlobalApplicationCommand(id, this.requests),
                                        appService::deleteGlobalApplicationCommand
                                )
                                .flatMap(t -> event.getMessage().getChannel().flatMap(mc -> mc.createMessage(t.toString())));
                            }
                        }
                        return Mono.empty();
                    })
                    .onErrorContinue((ex,obj) -> log.error("Error with text command: {}", obj, ex));
            return Flux.merge(chatInputHook, textHook);
        }).block();
    }

    private Mono<ReactionEmoji.Unicode> runOverwriteCommand(Mono<Long> appId, Function<Long, Flux<?>> func) {
        return appId
                .flatMapMany(func::apply)
                .collectList()
                .map(list -> {
                    log.info("Updated {} commands locally", list.size());
                    return ReactionEmoji.unicode("☑");
                })
                .onErrorResume(e -> {
                    log.error("Error updating commands locally", e);
                    return Mono.just(ReactionEmoji.unicode("☒"));
                });
    }

    private Mono<Tuple2<Integer, Integer>> runDiffOverwriteCommand(
            Mono<Long> appId,
            Function<Long, Flux<ApplicationCommandData>> getter,
            Function<Long, Flux<?>> updater,
            BiFunction<Long, Long, Mono<Void>> deleter) {
        return appId.flatMap(id -> {
                    return getter.apply(id)//appService.getGuildApplicationCommands(id, 423976318082744321L)
                            .collectList()
                            .map(acd -> diffService.findDiff(acd, this.requests))
                            .flatMap(diff -> {
                                var bulkDelete = Flux.fromIterable(diff.subtracted())
                                        .flatMap(cid -> deleter.apply(id, cid).thenReturn(1))
                                        .collectList()
                                        .map(List::size);
                                var bulkAdd = updater.apply(id)
                                        .collectList()
                                        .map(List::size);

                                return Mono.zip(bulkAdd, bulkDelete);
                            });
                });
//                .flatMap(t -> event.getMessage().getChannel().flatMap(mc -> mc.createMessage(t.toString())));
    }

    private boolean isMe(MessageCreateEvent evt) {
        return evt.getMessage()
                .getAuthor()
                .map(u -> MILO_ID.equals(u.getId()))
                .orElse(false);
    }
}
