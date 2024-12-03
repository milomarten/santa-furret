package com.github.milomarten.santa_furret.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Table(name = "participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventId", "participantId"})
})
public class SecretSantaParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "eventId")
    @ToString.Exclude
    private SecretSantaEvent event;
    private long participantId;

    private boolean okReceivingNsfw;
    private boolean okGivingNsfw;
}
