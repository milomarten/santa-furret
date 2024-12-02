package com.github.milomarten.santa_furret.repository;

import com.github.milomarten.santa_furret.models.exception.SecretSantaEvent;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretSantaEventRepository extends CrudRepository<SecretSantaEvent, UUID> {
    Optional<SecretSantaEvent> findByHomeGuildAndEventStartTimeLessThanAndEventEndTimeGreaterThan(long homeGuild, Instant time, Instant time2);
    Optional<SecretSantaEvent> findByHomeGuildAndEventStartTimeLessThanAndRegistrationEndTimeGreaterThan(long homeGuild, Instant time, Instant time2);
    List<SecretSantaEvent> findByHomeGuild(long homeGuild);
}
