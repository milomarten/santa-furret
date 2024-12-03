package com.github.milomarten.santa_furret.commands.parameter;

import discord4j.common.util.Snowflake;

import java.util.Optional;

public class GuildIdResolver {
    public static ParameterResolver<Snowflake> required() {
        return event -> event.getInteraction().getGuildId()
                .orElseThrow(() -> new ParameterValidationFailure("This command must be run in a server"));
    }

    public static ParameterResolver<Optional<Snowflake>> optional() {
        return event -> event.getInteraction().getGuildId();
    }
}
