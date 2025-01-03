package com.github.milomarten.santa_furret.commands.parameter;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

import java.util.Optional;

public class IdentityResolver {
    public static ParameterResolver<User> user() {
        return event -> event.getInteraction().getUser();
    }

    public static ParameterResolver<Optional<Member>> member() {
        return event -> event.getInteraction().getMember();
    }

    public static ParameterResolver<Member> memberRequired() {
        return event -> event.getInteraction().getMember()
                .orElseThrow(() -> new ParameterValidationFailure("This command must be run in a server!"));
    }
}
