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
    private final SecretSantaMatchupRepository matchupRepository;
    private final GenerateMatchupsService generateMatchupsService;

    private Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId, Snowflake ownerId) {
        return eventRepository.findByHomeGuildAndOrganizerAndStatusNot(
                guildId.asLong(), ownerId.asLong(), EventStatus.ENDED
        );
    }

    public List<SecretSantaMatchup> generateMatchups(UUID eventId) {
        var event = eventRepository.findById(eventId).orElseThrow();
        var matchups = generateMatchupsService.createMatchups(event.getParticipants());

        event.setStatus(EventStatus.SHOPPING);
        eventRepository.save(event);
        List<SecretSantaMatchup> returnList = new ArrayList<>();
        matchupRepository.saveAll(matchups).forEach(returnList::add);
        return returnList;
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
//        var evt = createEvent(
//                Snowflake.of(423976318082744321L),
//                Snowflake.of(248612704019808258L)
//        );
//        startEvent(evt.getId(), Snowflake.of(evt.getOrganizer()));
//
//        System.out.println("The dummy event ID is " + evt);

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
