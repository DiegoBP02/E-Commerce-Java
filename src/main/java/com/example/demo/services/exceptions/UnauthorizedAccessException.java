package com.example.demo.services.exceptions;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String msg) {
        super(msg);
    }
}
