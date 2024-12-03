package com.github.milomarten.santa_furret.repository;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretSantaParticipantRepository extends CrudRepository<SecretSantaParticipant, UUID> {
    List<SecretSantaParticipant> getByEventId(UUID eventId);
    Optional<SecretSantaParticipant> getByEventIdAndParticipantId(UUID eventId, long participantId);
    int deleteByEventIdAndParticipantId(UUID eventId, long participantId);
}
