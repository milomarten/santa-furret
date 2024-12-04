package com.github.milomarten.santa_furret.commands.parameter;

import com.github.milomarten.santa_furret.commands.parameter.validation.ValidationStep;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Parameter<T> {
    private final String[] path;
    private final Function<ApplicationCommandInteractionOptionValue, T> resolver;
    private final List<ValidationStep<T>> validationSteps = new ArrayList<>();

    public static Parameter<String> string(String... path) {
        validatePath(path);
        return new Parameter<>(path, ApplicationCommandInteractionOptionValue::asString);
    }

    public static Parameter<Boolean> bool(String... path) {
        validatePath(path);
        return new Parameter<>(path, ApplicationCommandInteractionOptionValue::asBoolean);
    }

    public static Parameter<Snowflake> snowflake(String... path) {
        validatePath(path);
        return new Parameter<>(path, ApplicationCommandInteractionOptionValue::asSnowflake);
    }

    private static void validatePath(String[] path) {
        if (path.length == 0) throw new IllegalArgumentException("path length must be >0");
    }

    public Parameter<T> validate(ValidationStep<T> step) {
        this.validationSteps.add(step);
        return this;
    }

    public <O> Parameter<O> convert(Function<T, O> func) {
        if (this.validationSteps.isEmpty()) {
            return new Parameter<>(path, this.resolver.andThen(func));
        } else {
            return new Parameter<>(path, new SquashedResolver<>(this.validationSteps, this.resolver, func, null));
        }
    }

    public <O> Parameter<O> convertLossy(Function<T, O> func, String errorMessage) {
        return new Parameter<>(path, new SquashedResolver<>(this.validationSteps, this.resolver, func, errorMessage));
    }

    Optional<ApplicationCommandInteractionOptionValue> followPath(ChatInputInteractionEvent event) {
        var current = event.getOption(path[0]);
        for (int i = 1; i < path.length; i++) {
            if (current.isPresent()) {
                current = current.get().getOption(path[i]);
            } else {
                break;
            }
        }
        return current.flatMap(ApplicationCommandInteractionOption::getValue);
    }

    public ParameterResolver<Optional<T>> optional() {
        return event -> followPath(event)
                .map(resolver)
                .map(item -> {
                    this.validationSteps.forEach(step -> step.validate(item));
                    return item;
                });
    }

    public ParameterResolver<T> optional(T defaultValue) {
        return event -> followPath(event)
                .map(resolver)
                .map(item -> {
                    this.validationSteps.forEach(step -> step.validate(item));
                    return item;
                })
                .orElse(defaultValue);
    }

    public ParameterResolver<T> required() {
        return event -> followPath(event)
                .map(resolver)
                .map(item -> {
                    this.validationSteps.forEach(step -> step.validate(item));
                    return item;
                })
                .orElseThrow(() -> {
                    String pathStr = String.join(".", path);
                    return new ParameterValidationFailure("Expected parameter " + pathStr);
                });
    }

    @RequiredArgsConstructor
    private static class SquashedResolver<T, O> implements Function<ApplicationCommandInteractionOptionValue, O> {
        private final List<ValidationStep<T>> validationSteps;
        private final Function<ApplicationCommandInteractionOptionValue, T> resolver;
        private final Function<T, O> finisher;
        private final String errorMessage;

        @Override
        public O apply(ApplicationCommandInteractionOptionValue value) {
            var item = this.resolver.apply(value);
            this.validationSteps.forEach(step -> step.validate(item));
            try {
                return finisher.apply(item);
            } catch (RuntimeException ex) {
                throw new ParameterValidationFailure(errorMessage, ex);
            }
        }
    }
}
