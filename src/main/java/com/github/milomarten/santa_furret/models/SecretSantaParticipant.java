package com.github.milomarten.santa_furret.models;

import com.github.milomarten.santa_furret.models.exception.SecretSantaEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Entity
@Data
@Table(name = "participant")
@IdClass(SecretSantaParticipant.Key.class)
public class SecretSantaParticipant {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId")
    @ToString.Exclude
    @Id
    private SecretSantaEvent event;
    @Id
    private long participantId;

    private boolean okReceivingNsfw;
    private boolean okGivingNsfw;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key {
        private UUID event;
        private long participantId;
    }
}
