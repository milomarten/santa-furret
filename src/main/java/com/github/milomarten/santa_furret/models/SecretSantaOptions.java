package com.github.milomarten.santa_furret.models;

import java.time.Instant;

public record SecretSantaOptions(Instant startDate, Instant drawDate, Instant giftDate, Instant endDate) {
}
