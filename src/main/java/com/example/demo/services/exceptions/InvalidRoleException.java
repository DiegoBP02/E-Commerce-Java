package com.example.demo.services.exceptions;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException() {
        super("Invalid role. Creating an admin role is not allowed.");
    }
}
