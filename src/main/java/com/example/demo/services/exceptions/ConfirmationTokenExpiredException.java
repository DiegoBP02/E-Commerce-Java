package com.example.demo.services.exceptions;

public class ConfirmationTokenExpiredException extends RuntimeException {
    public ConfirmationTokenExpiredException() {
        super("The confirmation token has expired," +
                "a new token has been sent to your email");
    }
}
