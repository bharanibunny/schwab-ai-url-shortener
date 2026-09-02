package com.schwab.urlshortener.util;

import com.schwab.urlshortener.exception.InvalidAliasException;
import com.schwab.urlshortener.exception.InvalidUrlException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

public class UrlValidatorUtil {

    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api",
            "swagger-ui",
            "swagger-ui.html",
            "v3",
            "h2-console",
            "actuator",
            "error",
            "favicon.ico",
            "health",
            "docs"
    );

    private UrlValidatorUtil() {
        // Utility class
    }

    public static void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL must not be empty or null.");
        }

        if (url.contains("\r") || url.contains("\n") || url.contains("\t")) {
            throw new InvalidUrlException("URL contains invalid control characters.");
        }

        if (url.length() > 2048) {
            throw new InvalidUrlException("URL exceeds maximum permitted length of 2048 characters.");
        }

        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new InvalidUrlException("URL scheme missing. Only 'http' and 'https' schemes are supported.");
            }
            String lowerScheme = scheme.toLowerCase();
            if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
                throw new InvalidUrlException("Unsupported URL scheme '" + scheme + "'. Only 'http' and 'https' are allowed.");
            }
            if (uri.getHost() == null) {
                throw new InvalidUrlException("URL host missing.");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL: " + e.getMessage());
        }
    }

    public static void validateExpiration(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new InvalidUrlException("Expiration date cannot be in the past or current instant.");
        }
    }

    public static String validateAndNormalizeCustomAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return null;
        }

        String normalized = customAlias.trim().toLowerCase(Locale.ROOT);

        if (RESERVED_ALIASES.contains(normalized)) {
            throw new InvalidAliasException("Custom alias '" + customAlias + "' is a reserved system keyword.");
        }

        if (normalized.length() < 4) {
            throw new InvalidAliasException("Custom alias must be at least 4 characters long.");
        }

        if (normalized.length() > 30) {
            throw new InvalidAliasException("Custom alias must not exceed 30 characters.");
        }

        if (!normalized.matches("^[a-z0-9_-]+$")) {
            throw new InvalidAliasException("Custom alias contains invalid characters. Only lowercase letters, numbers, hyphens, and underscores are allowed.");
        }

        if (normalized.startsWith("-") || normalized.startsWith("_") || normalized.endsWith("-") || normalized.endsWith("_")) {
            throw new InvalidAliasException("Custom alias cannot start or end with a hyphen or underscore.");
        }

        return normalized;
    }
}
