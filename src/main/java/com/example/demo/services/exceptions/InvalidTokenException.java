package com.example.demo.services.exceptions;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid token: No user was found with this token as the reset password token");
    }

    public InvalidTokenException(String msg) {
        super(msg);
    }
}
