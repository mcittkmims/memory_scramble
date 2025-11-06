package com.pr.memory_scramble.exception;

public class RestrictedCardAccessException extends RuntimeException {
    public RestrictedCardAccessException(String message) {
        super(message);
    }
}
