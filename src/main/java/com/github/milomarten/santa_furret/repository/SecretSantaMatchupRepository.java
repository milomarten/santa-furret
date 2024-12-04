package com.github.milomarten.santa_furret.repository;

import com.github.milomarten.santa_furret.models.SecretSantaMatchup;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface SecretSantaMatchupRepository extends CrudRepository<SecretSantaMatchup, UUID> {
}
