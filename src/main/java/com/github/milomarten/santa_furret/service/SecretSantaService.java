package com.github.milomarten.santa_furret.service;

import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.ParticipantOptions;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import com.github.milomarten.santa_furret.models.exception.EventInProgressException;
import com.github.milomarten.santa_furret.models.exception.EventNotInProgressException;
import com.github.milomarten.santa_furret.models.exception.ParticipantNotFoundException;
import com.github.milomarten.santa_furret.models.exception.RegistrationNotPermittedException;
import com.github.milomarten.santa_furret.repository.SecretSantaEventRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaMatchupRepository;
import com.github.milomarten.santa_furret.repository.SecretSantaParticipantRepository;
import discord4j.common.util.Snowflake;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SecretSantaService {
    private final SecretSantaEventRepository eventRepository;
    private final SecretSantaParticipantRepository participantRepository;
    private final SecretSantaMatchupRepository matchupRepository;

    public Optional<SecretSantaEvent> getCurrentEventFor(Snowflake guildId) {
        return eventRepository.findByHomeGuildAndStatusNot(
                guildId.asLong(), EventStatus.ENDED
        );
    }

    public SecretSantaParticipant addParticipant(Snowflake guildId, Snowflake participantId) {
        var event = getCurrentEventFor(guildId)
                .orElseThrow(EventNotInProgressException::new);
        if (event.getStatus() == EventStatus.REGISTRATION) {

            var p = new SecretSantaParticipant();
            p.setEvent(event);
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
                // While technically I can treat this as an insert (we have all the parameters)
                // It is not appropriate to assume a user wants to join the secret santa
                // if they try to update themselves.
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
                    .orElseThrow(ParticipantNotFoundException::new);
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
                .orElseThrow(ParticipantNotFoundException::new);
        user.getBlacklist().remove(blacklistId.asLong());
        return participantRepository.save(user);
    }
}
