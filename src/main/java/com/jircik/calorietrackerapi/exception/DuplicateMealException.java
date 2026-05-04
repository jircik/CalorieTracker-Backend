package com.jircik.calorietrackerapi.exception;

import lombok.Getter;

@Getter
public class DuplicateMealException extends RuntimeException {
    private final Long existingMealId;

    public DuplicateMealException(String message, Long existingMealId) {
        super(message);
        this.existingMealId = existingMealId;
    }

}
