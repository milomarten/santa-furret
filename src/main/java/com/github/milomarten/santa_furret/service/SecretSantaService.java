package com.github.milomarten.santa_furret.service;

import com.github.milomarten.santa_furret.models.*;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.RegistrationNotPermittedException;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SecretSantaService {
    private final SecretSantaEventRepository eventRepository;
    private final SecretSantaParticipantRepository participantRepository;

    private final EntityManager entityManager;

    public Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId) {
        var now = Instant.now();
        return eventRepository.findByHomeGuildAndEventStartTimeLessThanAndEventEndTimeGreaterThan(
                guildId.asLong(), now, now
        );
    }

    public Optional<SecretSantaEvent> getCurrentEventEligibleForRegistration(Snowflake guildId) {
        var now = Instant.now();
        return eventRepository.findByHomeGuildAndEventStartTimeLessThanAndRegistrationEndTimeGreaterThan(
                guildId.asLong(), now, now
        );
    }

    public Optional<SecretSantaEvent> getEvent(UUID uuid) {
        return eventRepository.findById(uuid);
    }

    public List<SecretSantaEvent> getAllEventsForGuild(Snowflake guildId) {
        return eventRepository.findByHomeGuild(guildId.asLong());
    }

    public SecretSantaEvent createEvent(Snowflake guildId, Snowflake ownerId, SecretSantaOptions options) {
        var eventMaybe = getCurrentEventFor(guildId);
        if (eventMaybe.isPresent()) { throw new EventInProgressException(); }

        var event = new SecretSantaEvent();
        event.setHomeGuild(guildId.asLong());
        event.setOrganizer(ownerId.asLong());
        event.setEventStartTime(options.startDate());
        event.setEventEndTime(options.endDate());
        event.setGiftingStartTime(options.giftDate());
        event.setRegistrationEndTime(options.drawDate());

        return eventRepository.save(event);
    }

    public void deleteEvent(UUID uuid) {
        eventRepository.deleteById(uuid);
    }

    public List<SecretSantaParticipant> getEventParticipants(UUID eventId) {
        return participantRepository.getByEventId(eventId);
    }

    public Optional<SecretSantaParticipant> getParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);

        return participantRepository.getByEventIdAndParticipantId(event.getId(), participantId.asLong());
    }

    public SecretSantaParticipant addParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getCurrentStatus() == EventStatus.REGISTRATION) {

            var p = new SecretSantaParticipant();
            p.setEvent(entityManager.getReference(SecretSantaEvent.class, event.getId()));
            p.setParticipantId(participantId.asLong());
            p.setOkGivingNsfw(false);
            p.setOkReceivingNsfw(false);

            return participantRepository.save(p);
        } else {
            throw new RegistrationNotPermittedException();
        }
    }

    public SecretSantaParticipant updateParticipant(Snowflake guildId, Snowflake participantId, ParticipantOptions options) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);

        var p = participantRepository.getByEventIdAndParticipantId(event.getId(), participantId.asLong())
                        .orElseThrow(RegistrationNotPermittedException::new);

        p.setOkGivingNsfw(options.okGivingNsfw());
        p.setOkReceivingNsfw(options.okReceivingNsfw());

        return participantRepository.save(p);
    }

    public boolean removeParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getCurrentStatus() == EventStatus.REGISTRATION) {
            var numDel = participantRepository.deleteByEventIdAndParticipantId(event.getId(), participantId.asLong());
            return numDel > 0;
        } else {
            throw new RegistrationNotPermittedException();
        }
    }

    @PostConstruct
    private void init() {
        createEvent(
                Snowflake.of(423976318082744321L),
                Snowflake.of(248612704019808258L),
                new SecretSantaOptions(
                        LocalDate.of(2024, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        LocalDate.of(2024, 12, 25).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        LocalDate.of(2024, 12, 28).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        LocalDate.of(2024, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
        );
    }
}
