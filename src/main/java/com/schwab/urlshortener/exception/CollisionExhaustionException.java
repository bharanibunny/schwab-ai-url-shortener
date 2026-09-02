package com.schwab.urlshortener.exception;

public class CollisionExhaustionException extends RuntimeException {
    public CollisionExhaustionException(int attempts) {
        super("Failed to generate a unique short code after " + attempts + " retry attempts due to code collisions.");
    }
}
