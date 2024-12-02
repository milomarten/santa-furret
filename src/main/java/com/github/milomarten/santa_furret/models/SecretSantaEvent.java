package com.github.milomarten.santa_furret.models;

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
    private Instant giftingStartTime;
    private Instant eventEndTime;

    public EventStatus getCurrentStatus() {
        var now = Instant.now();
        if (now.isAfter(eventEndTime)) {
            return EventStatus.ENDED;
        } else if (now.isAfter(giftingStartTime)) {
            return EventStatus.GIFTING;
        } else if (now.isAfter(registrationEndTime)) {
            return EventStatus.SHOPPING;
        } else if (now.isAfter(eventStartTime)) {
            return EventStatus.REGISTRATION;
        } else {
            return EventStatus.NOT_STARTED;
        }
    }
}
