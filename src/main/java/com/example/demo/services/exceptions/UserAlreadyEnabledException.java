package com.example.demo.services.exceptions;

public class UserAlreadyEnabledException extends RuntimeException{
    public UserAlreadyEnabledException() {
        super("The email confirmation has already been completed for this account.");
    }
}
