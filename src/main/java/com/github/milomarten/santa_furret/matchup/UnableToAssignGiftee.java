package com.github.milomarten.santa_furret.matchup;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;

public class UnableToAssignGiftee extends RuntimeException {
    private final SecretSantaParticipant santa;

    public UnableToAssignGiftee(SecretSantaParticipant santa, String reason) {
        super(reason);
        this.santa = santa;
    }
}
