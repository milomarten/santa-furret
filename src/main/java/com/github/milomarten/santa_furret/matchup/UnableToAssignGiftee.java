package com.github.milomarten.santa_furret.matchup;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import lombok.Getter;

@Getter
public class UnableToAssignGiftee extends RuntimeException {
    private final SecretSantaParticipant santa;

    public UnableToAssignGiftee(SecretSantaParticipant santa, String reason) {
        super(reason);
        this.santa = santa;
    }
}
