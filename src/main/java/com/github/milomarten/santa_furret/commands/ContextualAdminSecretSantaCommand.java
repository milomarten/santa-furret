package com.github.milomarten.santa_furret.commands;

import com.github.milomarten.santa_furret.commands.parameter.IdentityResolver;
import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import com.github.milomarten.santa_furret.models.SecretSantaEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

public abstract class ContextualAdminSecretSantaCommand extends ContextualSecretSantaCommand {
    @Override
    protected void hook(SecretSantaEvent event, ChatInputInteractionEvent cmd) {
        var user = IdentityResolver.user().resolve(cmd).getId();
        if (!event.validateOrganizer(user)) {
            throw new ParameterValidationFailure("Only the event runner can use this command.");
        }
        super.hook(event, cmd);
    }
}
