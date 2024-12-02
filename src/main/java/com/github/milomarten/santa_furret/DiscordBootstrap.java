package com.github.milomarten.santa_furret;

import discord4j.core.DiscordClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DiscordBootstrap implements ApplicationListener<ApplicationReadyEvent> {
    private final DiscordClient client;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        client.withGateway(gateway -> {
            return Mono.empty();
        }).block();
    }
}
