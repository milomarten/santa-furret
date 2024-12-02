package com.github.milomarten.santa_furret.repository;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface SecretSantaParticipantRepository extends CrudRepository<SecretSantaParticipant, SecretSantaParticipant.Key> {
    List<SecretSantaParticipant> getByEventId(UUID eventId);
}
