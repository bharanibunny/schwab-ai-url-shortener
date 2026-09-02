package com.schwab.urlshortener.unit;

import com.schwab.urlshortener.exception.InvalidAliasException;
import com.schwab.urlshortener.exception.InvalidUrlException;
import com.schwab.urlshortener.util.UrlValidatorUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorUtilTest {

    @Test
    void validateUrl_validHttpAndHttpsUrls_doesNotThrowException() {
        assertDoesNotThrow(() -> UrlValidatorUtil.validateUrl("http://example.com"));
        assertDoesNotThrow(() -> UrlValidatorUtil.validateUrl("https://example.com/some/very/long/path?param=value#anchor"));
        assertDoesNotThrow(() -> UrlValidatorUtil.validateUrl("HTTP://EXAMPLE.COM/UPPERCASE"));
    }

    @Test
    void validateUrl_nullOrBlankUrl_throwsInvalidUrlException() {
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl(null));
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl(""));
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("   "));
    }

    @Test
    void validateUrl_unsupportedScheme_throwsInvalidUrlException() {
        InvalidUrlException ex1 = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("ftp://example.com/file"));
        assertTrue(ex1.getMessage().contains("Unsupported URL scheme"));

        InvalidUrlException ex2 = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("javascript:alert(1)"));
        assertTrue(ex2.getMessage().contains("Unsupported URL scheme"));

        InvalidUrlException ex3 = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("mailto:user@example.com"));
        assertTrue(ex3.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void validateUrl_malformedUrl_throwsInvalidUrlException() {
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("htt&p://invalid-url-format"));
    }

    @Test
    void validateUrl_containsControlCharacters_throwsInvalidUrlException() {
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("https://example.com/path\r\nSet-Cookie:evil"));
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("https://example.com/path\nInjected-Header:1"));
        assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl("https://example.com/path\tWithTab"));
    }

    @Test
    void validateUrl_exact2048Chars_succeeds() {
        String exact2048Url = "https://example.com/" + "a".repeat(2028); // 20 + 2028 = 2048 chars
        assertEquals(2048, exact2048Url.length());
        assertDoesNotThrow(() -> UrlValidatorUtil.validateUrl(exact2048Url));
    }

    @Test
    void validateUrl_exceeds2048CharsByOne_throwsInvalidUrlException() {
        String url2049 = "https://example.com/" + "a".repeat(2029); // 20 + 2029 = 2049 chars
        assertEquals(2049, url2049.length());
        InvalidUrlException ex = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateUrl(url2049));
        assertTrue(ex.getMessage().contains("exceeds maximum permitted length"));
    }

    @Test
    void validateExpiration_futureDateOrNull_doesNotThrowException() {
        assertDoesNotThrow(() -> UrlValidatorUtil.validateExpiration(null));
        assertDoesNotThrow(() -> UrlValidatorUtil.validateExpiration(Instant.now().plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void validateExpiration_pastDateOrCurrentInstant_throwsInvalidUrlException() {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        InvalidUrlException ex = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateExpiration(past));
        assertTrue(ex.getMessage().contains("Expiration date cannot be in the past"));

        Instant currentInstant = Instant.now();
        InvalidUrlException exExact = assertThrows(InvalidUrlException.class, () -> UrlValidatorUtil.validateExpiration(currentInstant));
        assertTrue(exExact.getMessage().contains("Expiration date cannot be in the past"));
    }

    @Test
    void validateAndNormalizeCustomAlias_nullOrBlank_returnsNull() {
        assertNull(UrlValidatorUtil.validateAndNormalizeCustomAlias(null));
        assertNull(UrlValidatorUtil.validateAndNormalizeCustomAlias(""));
        assertNull(UrlValidatorUtil.validateAndNormalizeCustomAlias("   "));
    }

    @Test
    void validateAndNormalizeCustomAlias_validAndMixedCase_normalizesToLowercase() {
        assertEquals("my-profile", UrlValidatorUtil.validateAndNormalizeCustomAlias("My-Profile"));
        assertEquals("link_1234", UrlValidatorUtil.validateAndNormalizeCustomAlias("LINK_1234"));
    }

    @Test
    void validateAndNormalizeCustomAlias_lengthBoundaries() {
        // 4 chars (minimum permitted)
        assertEquals("a1-b", UrlValidatorUtil.validateAndNormalizeCustomAlias("a1-b"));

        // 3 chars (too short)
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("abc"));

        // 30 chars (maximum permitted)
        String alias30 = "a" + "b".repeat(28) + "c";
        assertEquals(30, alias30.length());
        assertEquals(alias30, UrlValidatorUtil.validateAndNormalizeCustomAlias(alias30));

        // 31 chars (too long)
        String alias31 = "a" + "b".repeat(29) + "c";
        assertEquals(31, alias31.length());
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias(alias31));
    }

    @Test
    void validateAndNormalizeCustomAlias_leadingTrailingHyphenOrUnderscore_throwsInvalidAliasException() {
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("-my-alias"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("_my-alias"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my-alias-"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my-alias_"));
    }

    @Test
    void validateAndNormalizeCustomAlias_invalidCharacters_throwsInvalidAliasException() {
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my.profile"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my/profile"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my@profile"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("my$profile"));
    }

    @Test
    void validateAndNormalizeCustomAlias_reservedAliases_throwsInvalidAliasException() {
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("api"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("API"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("swagger-ui"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("h2-console"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("actuator"));
        assertThrows(InvalidAliasException.class, () -> UrlValidatorUtil.validateAndNormalizeCustomAlias("error"));
    }
}
