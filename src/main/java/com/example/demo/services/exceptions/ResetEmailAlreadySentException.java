package com.example.demo.services.exceptions;

public class ResetEmailAlreadySentException extends RuntimeException {
    public ResetEmailAlreadySentException() {
        super("A reset email has already been sent to your email address. " +
                "Please check your inbox and spam folder.");
    }
}
