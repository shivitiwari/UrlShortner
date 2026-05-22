package com.systemdesign.url_shortener.exception;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("URL has expired for short code: " + shortCode);
    }
}

