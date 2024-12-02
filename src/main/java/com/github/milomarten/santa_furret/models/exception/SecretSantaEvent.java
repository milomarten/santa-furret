package com.github.milomarten.santa_furret.models.exception;

import com.github.milomarten.santa_furret.models.SecretSantaParticipant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "event")
public class SecretSantaEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private long organizer;
    private long homeGuild;
    @OneToMany(mappedBy = "event")
    @ToString.Exclude
    private List<SecretSantaParticipant> participants = new ArrayList<>();
    private Instant eventStartTime;
    private Instant registrationEndTime;
    private Instant eventEndTime;
}
