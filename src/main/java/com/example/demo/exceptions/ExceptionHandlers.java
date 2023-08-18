package com.example.demo.exceptions;


import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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
        String error = "Duplicate entry found. Please provide a unique value";
        HttpStatus status = HttpStatus.BAD_REQUEST;
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
    public ResponseEntity<StandardError> AccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        logger.error("Access denied exception:", e);
        String error = "Access denied";
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> MissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        logger.error("Access denied exception:", e);
        String error = "Missing request parameter";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> MethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        logger.error("Method argument type mismatch exception", e);
        String error = "Method argument type mismatch";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ;
        String message = "The value provided for the request parameter " + e.getName() + " is not valid.";
        StandardError err = new StandardError(Instant.now(), status.value(), error,
                message, request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

}
