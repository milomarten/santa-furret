package com.github.milomarten.santa_furret.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Setter
@Slf4j
public class CachedUsernameService {
    private GatewayDiscordClient client;

    private Map<Snowflake, Mono<String>> cache = new HashMap<>();

    public Mono<String> getUsername(Snowflake id) {
        return cache.computeIfAbsent(id, (snowflake) -> {
            return Mono.defer(() -> client.getUserById(id).map(User::getUsername))
                    .cache(t -> Duration.ofMinutes(30), ex -> Duration.ZERO, () -> Duration.ZERO)
                    .onErrorResume(ex -> {
                        log.error("Error retrieving username", ex);
                        return Mono.just("Snowflake<" + id.asLong() + ">");
                    });
        });
    }
}
