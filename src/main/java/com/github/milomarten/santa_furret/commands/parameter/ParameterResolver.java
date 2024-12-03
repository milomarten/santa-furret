package com.github.milomarten.santa_furret.commands.parameter;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public interface ParameterResolver<OUT> {
    OUT resolve(ChatInputInteractionEvent event);
}
