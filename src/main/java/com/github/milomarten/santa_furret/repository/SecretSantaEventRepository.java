package com.github.milomarten.santa_furret.repository;

import com.github.milomarten.santa_furret.models.EventStatus;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecretSantaEventRepository extends CrudRepository<SecretSantaEvent, UUID> {
    Optional<SecretSantaEvent> findByHomeGuildAndStatusNot(long homeGuild, EventStatus status);
    Optional<SecretSantaEvent> findByHomeGuildAndOrganizerAndStatusNot(long homeGuild, long organizer, EventStatus status);
    int deleteByIdAndOrganizer(UUID id, long organizer);
    Optional<SecretSantaEvent> findByIdAndOrganizer(UUID id, long organizer);
}
