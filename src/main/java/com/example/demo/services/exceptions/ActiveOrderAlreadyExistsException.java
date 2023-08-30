package com.example.demo.services.exceptions;

public class ActiveOrderAlreadyExistsException extends RuntimeException {
    public ActiveOrderAlreadyExistsException(String message) {
        super(message);
    }
}