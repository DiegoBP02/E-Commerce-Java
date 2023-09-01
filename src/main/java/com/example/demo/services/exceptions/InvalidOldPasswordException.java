package com.example.demo.services.exceptions;

public class InvalidOldPasswordException extends RuntimeException {
    public InvalidOldPasswordException() {
        super("Incorrect password. Please make sure the password is correct.");
    }
}
