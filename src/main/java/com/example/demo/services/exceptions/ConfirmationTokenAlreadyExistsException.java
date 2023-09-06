package com.example.demo.services.exceptions;

public class ConfirmationTokenAlreadyExistsException extends RuntimeException{
    public ConfirmationTokenAlreadyExistsException(long minutesUntilExpire) {
        super("A confirmation token for your account already exists. " +
                "Please check your email for the confirmation link " +
                "or wait " + minutesUntilExpire + "minutes to request a new one.");
    }
}
