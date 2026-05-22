package com.systemdesign.url_shortener.exception;

public class ShortCodeAlreadyExistsException extends RuntimeException {
    public ShortCodeAlreadyExistsException(String shortCode) {
        super("Short code already exists: " + shortCode);
    }
}

