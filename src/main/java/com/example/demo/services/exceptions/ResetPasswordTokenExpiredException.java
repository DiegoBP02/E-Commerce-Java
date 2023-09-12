package com.example.demo.services.exceptions;

public class ResetPasswordTokenExpiredException extends RuntimeException {
    public ResetPasswordTokenExpiredException() {
        super("The reset password token has expired. Please request another password reset email.");
        ;
    }
}
