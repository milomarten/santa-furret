package com.github.milomarten.santa_furret.commands.parameter.validation;

import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JakartaValidatorFactory {
    private final Validator validator;

    public <T> ValidationStep<T> asStep(Class<?>... groups) {
        return item -> {
            var invalidity = validator.validate(item, groups);
            if (!invalidity.isEmpty()) {
                throw new ParameterValidationFailure(invalidity.stream()
                        .map(ConstraintViolation::getMessage)
                        .findFirst()
                        .get());
            }
        };
    }
}
