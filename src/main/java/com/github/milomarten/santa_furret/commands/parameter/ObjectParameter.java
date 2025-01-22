package com.github.milomarten.santa_furret.commands.parameter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.milomarten.santa_furret.commands.parameter.validation.ValidationStep;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ObjectParameter<T> implements ParameterResolver<T>, ApplicationCommandOptionReader<T> {
    private final Supplier<T> constructor;
    private final List<FieldForComplexParameter<T, ?>> fields = new ArrayList<>();
    @Singular private final List<ValidationStep<T>> validationSteps = new ArrayList<>();

    public <U> ObjectParameter<T> field(String path, ApplicationCommandOptionReader<U> reader, BiConsumer<T, ? super U> setter) {
        this.fields.add(new FieldForComplexParameter<T, U>(path, reader, setter));
        return this;
    }

    public ObjectParameter<T> asBool(String path, BiConsumer<T, Boolean> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asBoolean),
                setter);
    }

    public ObjectParameter<T> asString(String path, BiConsumer<T, String> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asString),
                setter);
    }

    public ObjectParameter<T> asInteger(String path, BiConsumer<T, Integer> setter) {
        return field(path,
                data -> data.getValue().map(a -> (int)a.asLong()),
                setter);
    }

    public ObjectParameter<T> asLong(String path, BiConsumer<T, Long> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asLong),
                setter);
    }

    public ObjectParameter<T> asDouble(String path, BiConsumer<T, Double> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asDouble),
                setter);
    }

    public ObjectParameter<T> asSnowflake(String path, BiConsumer<T, Snowflake> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asSnowflake),
                setter);
    }

    public <U> ObjectParameter<T> asObject(String path, ObjectParameter<U> parser, BiConsumer<T, ? super U> setter) {
        return field(path,
                parser,
                setter);
    }

    public <U> ObjectParameter<T> asFromString(String path, Function<String, U> converter, BiConsumer<T, ? super U> setter) {
        return field(path,
                data -> data.getValue().map(ApplicationCommandInteractionOptionValue::asString)
                        .map(converter),
                setter);
    }

    @Override
    public T resolve(ChatInputInteractionEvent event) {
        var init = constructor.get();

        for (var field : fields) {
            field.read(event, init);
        }

        validationSteps.forEach(step -> step.validate(init));
        return init;
    }

    @Override
    public Optional<T> read(ApplicationCommandInteractionOption data) {
        var init = constructor.get();

        for (var field : fields) {
            field.read(data, init);
        }

        validationSteps.forEach(step -> step.validate(init));
        return Optional.of(init);
    }

    private record FieldForComplexParameter<T, U> (
            String name,
            ApplicationCommandOptionReader<U> reader,
            BiConsumer<T, ? super U> setter
    ) {
        public void read(ChatInputInteractionEvent event, T thing) {
            event.getOption(this.name)
                    .flatMap(this.reader::read)
                    .ifPresent(obj -> this.setter.accept(thing, obj));
        }

        public void read(ApplicationCommandInteractionOption event, T thing) {
            event.getOption(this.name)
                    .flatMap(this.reader::read)
                    .ifPresent(obj -> this.setter.accept(thing, obj));
        }
    }
}
