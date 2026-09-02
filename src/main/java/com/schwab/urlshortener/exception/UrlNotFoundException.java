package com.schwab.urlshortener.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortCode) {
        super("Short code '" + shortCode + "' not found.");
    }
}
