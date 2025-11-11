package com.pr.memory_scramble.exception.handler;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.InvalidCardAddressException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles exceptions when a player attempts to access a card that is
     * restricted.
     * Returns a 409 CONFLICT status with the exception message.
     *
     * @param ex the RestrictedCardAccessException that was thrown
     * @return a ResponseEntity with CONFLICT status and error message
     */
    @ExceptionHandler(RestrictedCardAccessException.class)
    public ResponseEntity<String> handleRestrictedCardAccess(RestrictedCardAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    /**
     * Handles exceptions when a player attempts to access a card that has been
     * removed.
     * Returns a 409 CONFLICT status with the exception message.
     *
     * @param ex the CardRemovedException that was thrown
     * @return a ResponseEntity with CONFLICT status and error message
     */
    @ExceptionHandler(CardRemovedException.class)
    public ResponseEntity<String> handleRestrictedCardAccess(CardRemovedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }

    /**
     * Handles exceptions when a player provides an invalid card address.
     * Returns a 400 BAD REQUEST status with the exception message.
     *
     * @param ex the InvalidCardAddressException that was thrown
     * @return a ResponseEntity with BAD_REQUEST status and error message
     */
    @ExceptionHandler(InvalidCardAddressException.class)
    public ResponseEntity<String> handleRestrictedCardAccess(InvalidCardAddressException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}
