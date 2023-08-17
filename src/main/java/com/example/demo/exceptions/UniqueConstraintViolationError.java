package com.example.demo.exceptions;

public class UniqueConstraintViolationError extends RuntimeException {
    public UniqueConstraintViolationError() {
        super("A record with the same value already exists. Duplicates are not allowed.");
    }

    public UniqueConstraintViolationError(String entity, String property) {
        super("A " + entity + " with the same " + property +
                " already exists. Duplicates are not allowed.");
    }
}

