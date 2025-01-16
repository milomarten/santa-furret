package com.github.milomarten.santa_furret.service.diff;

import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

public record Diff(
        List<ApplicationCommandRequest> added,
        List<ApplicationCommandData> same,
        List<Long> subtracted) {
}
