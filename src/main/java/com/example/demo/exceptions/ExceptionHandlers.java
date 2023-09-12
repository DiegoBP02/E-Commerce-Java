package com.example.demo.exceptions;


import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.demo.config.exceptions.UserNotEnabledException;
import com.example.demo.entities.exceptions.NoActiveOrderException;
import com.example.demo.services.exceptions.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class ExceptionHandlers {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlers.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> Exception(Exception e, HttpServletRequest request) {
        logger.error("Exception occurred:", e);
        String error = "Server error";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<StandardError> SignatureVerificationException
            (SignatureVerificationException e, HttpServletRequest request) {
        logger.error("Signature verification exception:", e);
        String error = "Invalid token signature";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(JWTDecodeException.class)
    public ResponseEntity<StandardError> JWTDecodeException
            (JWTDecodeException e, HttpServletRequest request) {
        logger.error("Database exception:", e);
        String error = "Error decoding JWT token";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<StandardError> TokenExpiredException
            (TokenExpiredException e, HttpServletRequest request) {
        logger.error("Token expired exception:", e);
        String error = "Token expired";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UniqueConstraintViolationError.class)
    public ResponseEntity<StandardError> UniqueConstraintViolationError
            (UniqueConstraintViolationError e, HttpServletRequest request) {
        logger.error("Unique constraint violation error:", e);
        String error = "Duplicate entry found";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> MethodArgumentNotValidException
            (MethodArgumentNotValidException e, HttpServletRequest request) {
        logger.error("Method argument not valid exception:", e);
        String error = "Invalid arguments";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        final List<String> errors = new ArrayList<>();
        for (final FieldError err : e.getBindingResult().getFieldErrors()) {
            errors.add(err.getField() + ": " + err.getDefaultMessage());
        }

        StandardError err = new StandardError(Instant.now(), status.value(), error,
                errors.toString(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> InvalidFormatException
            (HttpMessageNotReadableException e, HttpServletRequest request) {
        logger.error("Invalid format exception:", e);
        String error = "JSON parse error";

        if (e.getCause() instanceof InvalidFormatException invalidFormatException) {
            if (Enum.class.isAssignableFrom(invalidFormatException.getTargetType())) {
                Class<? extends Enum> enumClass = (Class<? extends Enum>)
                        invalidFormatException.getTargetType();
                String invalidValue = invalidFormatException.getValue().toString();
                String validValues = Arrays.stream(enumClass.getEnumConstants())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

                String message = String.format(
                        "%s is not one of the values accepted for Enum Class %s, valid values are: %s",
                        invalidValue, enumClass.getSimpleName(), validValues);
                HttpStatus status = HttpStatus.BAD_REQUEST;
                StandardError err = new StandardError(Instant.now(), status.value(), error,
                        message, request.getRequestURI());
                return ResponseEntity.status(status).body(err);
            }
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> DatabaseException(DatabaseException e, HttpServletRequest request) {
        logger.error("Database exception:", e);
        String error = "Database error";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> ResourceNotFoundException
            (ResourceNotFoundException e, HttpServletRequest request) {
        logger.error("Resource not found exception:", e);
        String error = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<StandardError> UnauthorizedAccessException
            (UnauthorizedAccessException e, HttpServletRequest request) {
        logger.error("Unauthorized access exception:", e);
        String error = "Access denied";
        HttpStatus status = HttpStatus.FORBIDDEN;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<StandardError> PropertyReferenceException
            (PropertyReferenceException e, HttpServletRequest request) {
        logger.error("Property reference exception:", e);
        String error = "The name of the property not found on the given type";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> AccessDeniedException
            (AccessDeniedException e, HttpServletRequest request) {
        logger.error("Access denied exception:", e);
        String error = "Access denied";
        HttpStatus status = HttpStatus.FORBIDDEN;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> MissingServletRequestParameterException
            (MissingServletRequestParameterException e, HttpServletRequest request) {
        logger.error("Missing servlet request parameter exception:", e);
        String error = "Missing request parameter";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> MethodArgumentTypeMismatchException
            (MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        logger.error("Method argument type mismatch exception", e);
        String error = "Method argument type mismatch";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ;
        String message = "The value provided for the request parameter " + e.getName() + " is not valid.";
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(StripeErrorException.class)
    public ResponseEntity<StandardError> StripeErrorException
            (StripeErrorException e, HttpServletRequest request) {
        logger.error("Stripe error occurred:", e);
        String error = "Stripe error";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<StandardError> InvalidOrderException
            (InvalidOrderException e, HttpServletRequest request) {
        logger.error("Invalid order exception:", e);
        String error = "Invalid order";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<StandardError> InsufficientBalanceException
            (InsufficientBalanceException e, HttpServletRequest request) {
        logger.error("Insufficient balance exception:", e);
        String error = "Insufficient balance";
        HttpStatus status = HttpStatus.PAYMENT_REQUIRED;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }


    @ExceptionHandler(ActiveOrderAlreadyExistsException.class)
    public ResponseEntity<StandardError> ActiveOrderAlreadyExistsException
            (ActiveOrderAlreadyExistsException e, HttpServletRequest request) {
        logger.error("Active order already exists exception :", e);
        String error = "A conflict occurred: An active order already exists for this customer.";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(NoActiveOrderException.class)
    public ResponseEntity<StandardError> NoActiveOrderException
            (NoActiveOrderException e, HttpServletRequest request) {
        logger.error("No active order exists:", e);
        String error = "The customer has no active order";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<StandardError> UserNotFoundException
            (UserNotFoundException e, HttpServletRequest request) {
        logger.error("User not found exception:", e);
        String error = "User not found";
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> BadCredentialsException
            (BadCredentialsException e, HttpServletRequest request) {
        logger.error("Bad credentials exception:", e);
        String error = "Bad credentials";
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<StandardError> LockedException
            (LockedException e, HttpServletRequest request) {
        logger.error("Locked exception:", e);
        String error = "Locked account";
        HttpStatus status = HttpStatus.LOCKED;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidOldPasswordException.class)
    public ResponseEntity<StandardError> InvalidOldPasswordException
            (InvalidOldPasswordException e, HttpServletRequest request) {
        logger.error("Invalid old password exception:", e);
        String error = "Invalid password";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<StandardError> InvalidTokenException
            (InvalidTokenException e, HttpServletRequest request) {
        logger.error("Invalid token exception:", e);
        String error = "Error during token validation";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<StandardError> EmailSendException
            (EmailSendException e, HttpServletRequest request) {
        logger.error("Email send exception:", e);
        String error = "Error during email sending";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UserAlreadyEnabledException.class)
    public ResponseEntity<StandardError> UserAlreadyEnabledException
            (UserAlreadyEnabledException e, HttpServletRequest request) {
        logger.error("User already enabled exception:", e);
        String error = "User already enabled";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<StandardError> EntityNotFoundException
            (EntityNotFoundException e, HttpServletRequest request) {
        logger.error("Entity not found exception:", e);
        String error = "Resource not found";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ConfirmationTokenAlreadyExistsException.class)
    public ResponseEntity<StandardError> ConfirmationTokenAlreadyExistsException
            (ConfirmationTokenAlreadyExistsException e, HttpServletRequest request) {
        logger.error("Confirmation token already exists exception:", e);
        String error = "Confirmation token already exists for this user";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ConfirmationTokenExpiredException.class)
    public ResponseEntity<StandardError> ConfirmationTokenExpiredException
            (ConfirmationTokenExpiredException e, HttpServletRequest request) {
        logger.error("Confirmation token expired exception:", e);
        String error = "The confirmation token has expired";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UserNotEnabledException.class)
    public ResponseEntity<StandardError> UserNotEnabledException
            (UserNotEnabledException e, HttpServletRequest request) {
        logger.error("User not enabled exception:", e);
        String error = "User not enabled. Only enabled users can access this route.";
        HttpStatus status = HttpStatus.FORBIDDEN;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ProductNotPurchasedException.class)
    public ResponseEntity<StandardError> ProductNotPurchasedException
            (ProductNotPurchasedException e, HttpServletRequest request) {
        logger.error("Product not purchased exception:", e);
        String error = "Product not purchased";
        HttpStatus status = HttpStatus.FORBIDDEN;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<StandardError> ClassCastException
            (ClassCastException e, HttpServletRequest request) {
        logger.error("Class cast exception exception:", e);
        String error = "Invalid casting";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ResetEmailAlreadySentException.class)
    public ResponseEntity<StandardError> ResetEmailAlreadySentException
            (ResetEmailAlreadySentException e, HttpServletRequest request) {
        logger.error("Reset email already sent exception:", e);
        String error = "Reset email already sent";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ResetPasswordTokenExpiredException.class)
    public ResponseEntity<StandardError> ResetPasswordTokenExpired
            (ResetPasswordTokenExpiredException e, HttpServletRequest request) {
        logger.error("Class cast exception exception:", e);
        String error = "Reset password token expired";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(),
                error, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

}
