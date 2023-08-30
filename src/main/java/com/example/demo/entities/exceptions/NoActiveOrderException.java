package com.example.demo.entities.exceptions;

public class NoActiveOrderException extends RuntimeException {
    public NoActiveOrderException(String message) {
        super(message);
    }
}
