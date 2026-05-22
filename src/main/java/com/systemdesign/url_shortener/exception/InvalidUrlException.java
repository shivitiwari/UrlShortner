package com.systemdesign.url_shortener.exception;

public class InvalidUrlException extends RuntimeException {
    public InvalidUrlException(String url) {
        super("Invalid URL format: " + url);
    }
}

