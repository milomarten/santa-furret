package com.github.milomarten.santa_furret;

import com.github.milomarten.santa_furret.models.ParticipantOptions;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.models.SecretSantaOptions;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import com.github.milomarten.santa_furret.service.SecretSantaService;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class Tester {
    private final SecretSantaService service;

    @PostConstruct
    public void test() {
        var event = service.createEvent(
                Snowflake.of(423976318082744321L),
                Snowflake.of(248612704019808258L),
                new SecretSantaOptions(
                        Instant.now(),
                        Instant.now().plus(6, ChronoUnit.DAYS),
                        Instant.now().plus(25, ChronoUnit.DAYS)
                )
        );

        service.addParticipant(event.getId(), Snowflake.of(247588838216695809L), ParticipantOptions.YES);

        System.out.println(service.getEventParticipants(event.getId()));

        service.removeParticipant(event.getId(), Snowflake.of(247588838216695809L));

        System.out.println(service.getEventParticipants(event.getId()));
    }
}
