package com.example.demo.services.exceptions;

public class InvalidPaymentStatusException extends RuntimeException {
    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}

