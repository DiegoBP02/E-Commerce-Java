package com.example.demo.services.exceptions;

public class StripeErrorException extends RuntimeException {
    public StripeErrorException(String message) {
        super(message);
    }
}

