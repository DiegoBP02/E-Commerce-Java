package com.example.demo.controller.exceptions;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("Rate Limit Exceeded: You have reached the maximum number of allowed requests. " +
                "Please wait before making additional requests.");
    }
}