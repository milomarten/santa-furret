package com.github.milomarten.santa_furret.commands.parameter.validation;

import com.github.milomarten.santa_furret.commands.parameter.ParameterValidationFailure;

public interface ValidationStep<T> {
    void validate(T item) throws ParameterValidationFailure;
}
