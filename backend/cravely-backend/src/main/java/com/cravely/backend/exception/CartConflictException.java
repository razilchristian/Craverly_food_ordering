package com.cravely.backend.exception;

public class CartConflictException extends RuntimeException {

    public CartConflictException(String message) {
        super(message);
    }
}

