package com.pr.memory_scramble.exception.handler;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestrictedCardAccessException.class)
    public ResponseEntity<String> handleRestrictedCardAccess(RestrictedCardAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    @ExceptionHandler(CardRemovedException.class)
    public ResponseEntity<String> handleRestrictedCardAccess(CardRemovedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }
}
