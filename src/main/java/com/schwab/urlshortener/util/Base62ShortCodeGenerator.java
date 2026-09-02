package com.schwab.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base62ShortCodeGenerator {

    private static final String BASE62_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int BASE = BASE62_CHARACTERS.length();
    private static final int DEFAULT_CODE_LENGTH = 7;

    private final SecureRandom random;

    public Base62ShortCodeGenerator() {
        this.random = new SecureRandom();
    }

    public String generateCode() {
        return generateCode(DEFAULT_CODE_LENGTH);
    }

    public String generateCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Code length must be greater than zero");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(BASE);
            sb.append(BASE62_CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
