package com.github.milomarten.santa_furret.commands.parameter;

import discord4j.core.object.command.ApplicationCommandInteractionOption;

import java.util.Optional;

public interface ApplicationCommandOptionReader<T> {
    Optional<T> read(ApplicationCommandInteractionOption data);
}
