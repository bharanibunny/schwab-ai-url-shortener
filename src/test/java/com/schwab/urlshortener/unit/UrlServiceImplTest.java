package com.schwab.urlshortener.unit;

import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.ShortUrlResponse;
import com.schwab.urlshortener.entity.ShortUrl;
import com.schwab.urlshortener.exception.AliasAlreadyExistsException;
import com.schwab.urlshortener.exception.CollisionExhaustionException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.exception.UrlNotFoundException;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import com.schwab.urlshortener.service.impl.UrlServiceImpl;
import com.schwab.urlshortener.util.Base62ShortCodeGenerator;
import com.schwab.urlshortener.repository.ShortUrlSaveHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private Base62ShortCodeGenerator codeGenerator;

    @Mock
    private ShortUrlSaveHelper saveHelper;

    @InjectMocks
    private UrlServiceImpl urlService;

    @BeforeEach
    void setUp() {
        urlService.setMaxRetries(5);
        urlService.setCodeLength(7);
        urlService.setBaseUrl("http://localhost:8080");
    }

    @Test
    void createShortUrl_customAliasSuccess() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/profile", null, "My-Profile");
        when(shortUrlRepository.existsByShortCodeIgnoreCase("my-profile")).thenReturn(false);

        ShortUrl savedEntity = new ShortUrl("my-profile", "https://example.com/profile", Instant.now(), null);
        savedEntity.setId(10L);
        when(saveHelper.saveInNewTransaction(any(ShortUrl.class))).thenReturn(savedEntity);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("my-profile", response.getShortCode());
        assertEquals("http://localhost:8080/my-profile", response.getShortUrl());
        verify(codeGenerator, never()).generateCode(anyInt());
        verify(saveHelper, times(1)).saveInNewTransaction(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_customAliasCollision_throwsAliasAlreadyExistsException() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/profile", null, "my-profile");
        when(shortUrlRepository.existsByShortCodeIgnoreCase("my-profile")).thenReturn(true);

        assertThrows(AliasAlreadyExistsException.class, () -> urlService.createShortUrl(request));
        verify(codeGenerator, never()).generateCode(anyInt());
        verify(saveHelper, never()).saveInNewTransaction(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_successWithoutCollisions() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/long-url", null);
        when(codeGenerator.generateCode(7)).thenReturn("aB7kP2x");
        when(shortUrlRepository.existsByShortCode("aB7kP2x")).thenReturn(false);

        ShortUrl savedEntity = new ShortUrl("aB7kP2x", "https://example.com/long-url", Instant.now(), null);
        savedEntity.setId(1L);
        when(saveHelper.saveInNewTransaction(any(ShortUrl.class))).thenReturn(savedEntity);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("aB7kP2x", response.getShortCode());
        assertEquals("http://localhost:8080/aB7kP2x", response.getShortUrl());
        assertEquals("https://example.com/long-url", response.getOriginalUrl());
        assertEquals(0, response.getClickCount());
        verify(saveHelper, times(1)).saveInNewTransaction(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_collisionRetrySuccess() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/long-url", null);
        when(codeGenerator.generateCode(7))
                .thenReturn("code1") // first attempt collides
                .thenReturn("code2"); // second attempt succeeds

        when(shortUrlRepository.existsByShortCode("code1")).thenReturn(true);
        when(shortUrlRepository.existsByShortCode("code2")).thenReturn(false);

        ShortUrl savedEntity = new ShortUrl("code2", "https://example.com/long-url", Instant.now(), null);
        savedEntity.setId(2L);
        when(saveHelper.saveInNewTransaction(any(ShortUrl.class))).thenReturn(savedEntity);

        ShortUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("code2", response.getShortCode());
        verify(codeGenerator, times(2)).generateCode(7);
        verify(saveHelper, times(1)).saveInNewTransaction(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_collisionExhaustion_throwsCollisionExhaustionException() {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/long-url", null);
        when(codeGenerator.generateCode(7)).thenReturn("collide");
        when(shortUrlRepository.existsByShortCode("collide")).thenReturn(true);

        assertThrows(CollisionExhaustionException.class, () -> urlService.createShortUrl(request));
        verify(codeGenerator, times(5)).generateCode(7);
        verify(shortUrlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void resolveAndRedirect_successfulResolution() {
        ShortUrl shortUrl = new ShortUrl("aB7kP2x", "https://example.com/target", Instant.now(), null);
        when(shortUrlRepository.findByShortCode("aB7kP2x")).thenReturn(Optional.of(shortUrl));

        String targetUrl = urlService.resolveAndRedirect("aB7kP2x");

        assertEquals("https://example.com/target", targetUrl);
        verify(shortUrlRepository, times(1)).incrementClickCount("aB7kP2x");
    }

    @Test
    void resolveAndRedirect_unknownCode_throwsUrlNotFoundException() {
        when(shortUrlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.resolveAndRedirect("unknown"));
        verify(shortUrlRepository, never()).incrementClickCount(anyString());
    }

    @Test
    void resolveAndRedirect_expiredCode_throwsUrlExpiredException() {
        Instant pastExpiration = Instant.now().minus(1, ChronoUnit.HOURS);
        ShortUrl expiredUrl = new ShortUrl("expired", "https://example.com/target", Instant.now().minus(2, ChronoUnit.HOURS), pastExpiration);
        when(shortUrlRepository.findByShortCode("expired")).thenReturn(Optional.of(expiredUrl));

        assertThrows(UrlExpiredException.class, () -> urlService.resolveAndRedirect("expired"));
        verify(shortUrlRepository, never()).incrementClickCount(anyString());
    }

    @Test
    void resolveAndRedirect_futureExpiration_allowsRedirect() {
        Instant futureExpiration = Instant.now().plus(1, ChronoUnit.HOURS);
        ShortUrl futureUrl = new ShortUrl("future", "https://example.com/target", Instant.now(), futureExpiration);
        when(shortUrlRepository.findByShortCode("future")).thenReturn(Optional.of(futureUrl));

        String redirectUrl = urlService.resolveAndRedirect("future");
        assertEquals("https://example.com/target", redirectUrl);
        verify(shortUrlRepository, times(1)).incrementClickCount("future");
    }

    @Test
    void resolveAndRedirect_exactExpirationInstant_treatedAsExpired() {
        // Expiration instant set slightly in the past so !expiresAt.isAfter(now) evaluates to true deterministically
        Instant exactExpiration = Instant.now();
        ShortUrl exactUrl = new ShortUrl("exact", "https://example.com/target", exactExpiration.minus(1, ChronoUnit.HOURS), exactExpiration);
        when(shortUrlRepository.findByShortCode("exact")).thenReturn(Optional.of(exactUrl));

        assertThrows(UrlExpiredException.class, () -> urlService.resolveAndRedirect("exact"));
        verify(shortUrlRepository, never()).incrementClickCount(anyString());
    }

    @Test
    void getAnalytics_successfulRetrieval() {
        ShortUrl shortUrl = new ShortUrl("aB7kP2x", "https://example.com/target", Instant.now(), null);
        shortUrl.setClickCount(42L);
        when(shortUrlRepository.findByShortCode("aB7kP2x")).thenReturn(Optional.of(shortUrl));

        AnalyticsResponse response = urlService.getAnalytics("aB7kP2x");

        assertNotNull(response);
        assertEquals("aB7kP2x", response.getShortCode());
        assertEquals("https://example.com/target", response.getOriginalUrl());
        assertEquals(42L, response.getClickCount());
    }

    @Test
    void getAnalytics_expiredUrl_returns200OKWithAnalytics() {
        Instant pastExpiration = Instant.now().minus(1, ChronoUnit.DAYS);
        ShortUrl expiredUrl = new ShortUrl("expired-analytics", "https://example.com/expired", Instant.now().minus(2, ChronoUnit.DAYS), pastExpiration);
        expiredUrl.setClickCount(15L);
        when(shortUrlRepository.findByShortCode("expired-analytics")).thenReturn(Optional.of(expiredUrl));

        AnalyticsResponse response = urlService.getAnalytics("expired-analytics");

        assertNotNull(response);
        assertEquals("expired-analytics", response.getShortCode());
        assertEquals(15L, response.getClickCount());
        assertEquals(pastExpiration, response.getExpiresAt());
    }
}
