package com.github.milomarten.santa_furret.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Entity
@Data
@Table(name = "matchup")
@NoArgsConstructor
@AllArgsConstructor
public class SecretSantaMatchup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "santaId")
    @ToString.Exclude
    private SecretSantaParticipant santa;

    @OneToOne
    @JoinColumn(name = "gifteeId")
    @ToString.Exclude
    private SecretSantaParticipant giftee;

    @ManyToOne
    @JoinColumn(name = "eventId")
    @ToString.Exclude
    private SecretSantaEvent event;

    private boolean gifteeReceivedGift;
}
