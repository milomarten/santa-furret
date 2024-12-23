package com.github.milomarten.santa_furret.service;

import com.github.milomarten.santa_furret.matchup.GenerateMatchupsService;
import com.github.milomarten.santa_furret.models.*;
import com.github.milomarten.santa_furret.models.exception.*;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaMatchupRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SecretSantaService {
    private final SecretSantaEventRepository eventRepository;
    private final SecretSantaParticipantRepository participantRepository;
    private final SecretSantaMatchupRepository matchupRepository;
    private final GenerateMatchupsService generateMatchupsService;

    private final EntityManager entityManager;

    public Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId) {
        var now = Instant.now();
        return eventRepository.findByHomeGuildAndStatusNot(
                guildId.asLong(), EventStatus.ENDED
        );
    }

    public SecretSantaEvent createEvent(Snowflake guildId, Snowflake ownerId) {
        var eventMaybe = getCurrentEventFor(guildId);
        if (eventMaybe.isPresent()) { throw new EventInProgressException(); }

        var event = new SecretSantaEvent();
        event.setHomeGuild(guildId.asLong());
        event.setOrganizer(ownerId.asLong());
        event.setStatus(EventStatus.NOT_STARTED);

        return eventRepository.save(event);
    }

    public SecretSantaEvent startEvent(UUID eventId, Snowflake ownerId) {
        var eventMaybe = eventRepository.findByIdAndOrganizer(eventId, ownerId.asLong());
        if (eventMaybe.isEmpty()) { throw new NoSuchEvent(); }

        var event = eventMaybe.get();
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            throw new EventInProgressException();
        }

        event.setStatus(EventStatus.REGISTRATION);
        return eventRepository.save(event);
    }

    public boolean deleteEvent(UUID uuid, Snowflake ownerId) {
        return eventRepository.deleteByIdAndOrganizer(uuid, ownerId.asLong()) > 0;
    }

    public SecretSantaParticipant addParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getStatus() == EventStatus.REGISTRATION) {

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

    public List<SecretSantaParticipant> getParticipants(Snowflake guildId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);

        return participantRepository.getByEventId(event.getId());
    }

    public boolean removeParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getStatus() == EventStatus.REGISTRATION) {
            var numDel = participantRepository.deleteByEventIdAndParticipantId(event.getId(), participantId.asLong());
            return numDel > 0;
        } else {
            throw new RegistrationNotPermittedException();
        }
    }

    public SecretSantaParticipant addBlacklist(Snowflake guildId, Snowflake participantId, Snowflake blacklistId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getStatus() == EventStatus.REGISTRATION) {
            var user = participantRepository.getByEventIdAndParticipantId(event.getId(), participantId.asLong())
                    .orElseThrow(ParticipantNotRegisteredException::new);
            user.getBlacklist().add(blacklistId.asLong());
            return participantRepository.save(user);
        } else {
            throw new EventInProgressException();
        }
    }

    public SecretSantaParticipant removeBlacklist(Snowflake guildId, Snowflake participantId, Snowflake blacklistId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);

        var user = participantRepository.getByEventIdAndParticipantId(event.getId(), participantId.asLong())
                .orElseThrow(ParticipantNotRegisteredException::new);
        user.getBlacklist().remove(blacklistId.asLong());
        return participantRepository.save(user);
    }

    public List<SecretSantaMatchup> generateMatchups(Snowflake guildId, Snowflake ownerId) {
        var event = getCurrentEventFor(guildId)
                .filter(sse -> sse.getOrganizer() == ownerId.asLong())
                .orElseThrow(EventNotInProgressException::new);
        if (event.getStatus() == EventStatus.REGISTRATION) {
            var matchups = generateMatchupsService.createMatchups(event.getParticipants());

            event.setStatus(EventStatus.GIFTING);
            eventRepository.save(event);
            List<SecretSantaMatchup> returnList = new ArrayList<>();
            matchupRepository.saveAll(matchups).forEach(returnList::add);
            return returnList;
        } else {
            throw new InvalidEventState();
        }
    }

    @PostConstruct
    private void init() {
        var evt = createEvent(
                Snowflake.of(423976318082744321L),
                Snowflake.of(248612704019808258L)
        );
        startEvent(evt.getId(), Snowflake.of(evt.getOrganizer()));

        System.out.println("The dummy event ID is " + evt);

        var participant = new SecretSantaParticipant();
        participant.setParticipantId(248612704019808258L);
        participant.setEvent(evt);
        participant = participantRepository.save(participant);

        var participant2 = new SecretSantaParticipant();
        participant.setParticipantId(252289670522601472L);
        participant.setEvent(evt);
        participant2 = participantRepository.save(participant2);

        var matchup = new SecretSantaMatchup();
        matchup.setSanta(participant);
        matchup.setGiftee(participant2);
        matchup.setEvent(evt);
        matchup = matchupRepository.save(matchup);
    }
}
