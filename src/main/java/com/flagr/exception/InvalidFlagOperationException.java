package com.flagr.exception;

public class InvalidFlagOperationException extends RuntimeException {
    public InvalidFlagOperationException(String message) {
        super(message);
    }
}
