package com.github.milomarten.santa_furret.service;

import com.github.milomarten.santa_furret.matchup.GenerateMatchupsService;
import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.models.SecretSantaMatchup;
import com.github.milomarten.santa_furret.models.exception.*;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaMatchupRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminSecretSantaService {
    private final SecretSantaEventRepository eventRepository;
    private final SecretSantaParticipantRepository participantRepository;
    private final SecretSantaMatchupRepository matchupRepository;
    private final GenerateMatchupsService generateMatchupsService;

    private Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId, Snowflake ownerId) {
        return eventRepository.findByHomeGuildAndOrganizerAndStatusNot(
                guildId.asLong(), ownerId.asLong(), EventStatus.ENDED
        );
    }

    public SecretSantaEvent createEvent(Snowflake guildId, Snowflake ownerId) {
        var eventMaybe = getCurrentEventFor(guildId, ownerId);
        if (eventMaybe.isPresent()) { throw new EventInProgressException(); }

        var event = new SecretSantaEvent();
        event.setHomeGuild(guildId.asLong());
        event.setOrganizer(ownerId.asLong());
        event.setStatus(EventStatus.NOT_STARTED);

        return eventRepository.save(event);
    }

    public SecretSantaEvent startEvent(UUID eventId, Snowflake ownerId) {
        var eventMaybe = eventRepository.findByIdAndOrganizer(eventId, ownerId.asLong());
        if (eventMaybe.isEmpty()) { throw new EventNotFoundException(); }

        var event = eventMaybe.get();
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            throw new EventInProgressException();
        }

        event.setStatus(EventStatus.REGISTRATION);
        return eventRepository.save(event);
    }

    public SecretSantaEvent endEvent(UUID eventId, Snowflake ownerId) {
        var eventMaybe = eventRepository.findByIdAndOrganizer(eventId, ownerId.asLong());
        if (eventMaybe.isEmpty()) { throw new EventNotFoundException(); }

        var event = eventMaybe.get();
        event.setStatus(EventStatus.ENDED);
        return eventRepository.save(event);
    }

    public boolean deleteEvent(UUID uuid, Snowflake ownerId) {
        return eventRepository.deleteByIdAndOrganizer(uuid, ownerId.asLong()) > 0;
    }

    public List<SecretSantaMatchup> generateMatchups(Snowflake guildId, Snowflake ownerId) {
        var event = getCurrentEventFor(guildId, ownerId)
                .orElseThrow(EventNotInProgressException::new);

        if (event.getStatus() == EventStatus.REGISTRATION) {
            var matchups = generateMatchupsService.createMatchups(event.getParticipants());

            event.setStatus(EventStatus.SHOPPING);
            eventRepository.save(event);
            List<SecretSantaMatchup> returnList = new ArrayList<>();
            matchupRepository.saveAll(matchups).forEach(returnList::add);
            return returnList;
        } else {
            throw new MatchupNotPermittedException();
        }
    }

    public void beginGifting(Snowflake guildId, Snowflake ownerId) {
        var event = getCurrentEventFor(guildId, ownerId)
                .orElseThrow(EventNotInProgressException::new);

        if (event.getStatus() == EventStatus.SHOPPING) {
            event.setStatus(EventStatus.GIFTING);
            eventRepository.save(event);
        } else {
            throw new GiftingNotPermittedException();
        }
    }

    public List<SecretSantaMatchup> getMatchups(Snowflake guildId, Snowflake ownerId) {
        var event = getCurrentEventFor(guildId, ownerId)
                .orElseThrow(EventNotInProgressException::new);

        if (event.getStatus() == EventStatus.SHOPPING) {
            return new ArrayList<>(event.getMatchups());
        } else {
            throw new GiftingNotPermittedException();
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

//        var participant = new SecretSantaParticipant();
//        participant.setParticipantId(248612704019808258L);
//        participant.setEvent(evt);
//        participant = participantRepository.save(participant);
//
//        var participant2 = new SecretSantaParticipant();
//        participant2.setParticipantId(252289670522601472L);
//        participant2.setEvent(evt);
//        participant2 = participantRepository.save(participant2);
//
//        var matchup = new SecretSantaMatchup();
//        matchup.setSanta(participant);
//        matchup.setGiftee(participant2);
//        matchup.setEvent(evt);
//        matchup = matchupRepository.save(matchup);
//
//        evt.setStatus(EventStatus.SHOPPING);
//        eventRepository.save(evt);
    }
}
