package com.github.milomarten.santa_furret.service;

import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecretSantaService {
    private final SecretSantaEventRepository eventRepository;
    private final SecretSantaParticipantRepository participantRepository;

    public Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId) {
        return eventRepository.findByHomeGuildAndStatusNot(
                guildId.asLong(), EventStatus.ENDED
        );
    }

    public List<SecretSantaParticipant> getParticipants(Snowflake guildId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);

        return event.getParticipants();
    }
}
