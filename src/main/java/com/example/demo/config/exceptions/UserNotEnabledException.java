package com.example.demo.config.exceptions;

public class UserNotEnabledException extends RuntimeException{
    public UserNotEnabledException() {
        super("User account is not enabled. Please confirm your email to enable your account.");
    }
}
