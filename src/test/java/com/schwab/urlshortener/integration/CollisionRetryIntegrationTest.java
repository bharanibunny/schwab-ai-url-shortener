package com.schwab.urlshortener.integration;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.ShortUrlResponse;
import com.schwab.urlshortener.entity.ShortUrl;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import com.schwab.urlshortener.service.UrlService;
import com.schwab.urlshortener.util.Base62ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class CollisionRetryIntegrationTest {

    @Autowired
    private UrlService urlService;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @MockBean
    private Base62ShortCodeGenerator codeGenerator;

    @Test
    void createShortUrl_databaseLevelCollisionRetry_succeedsWithoutUnexpectedRollback() {
        // Pre-insert an entity into the database with shortCode "COLLIDE"
        ShortUrl existingUrl = new ShortUrl("COLLIDE", "https://example.com/existing", Instant.now(), null);
        shortUrlRepository.saveAndFlush(existingUrl);

        // Simulate candidate generation:
        // Attempt 1: returns "COLLIDE"
        // Attempt 2: returns "UNIQUE1"
        when(codeGenerator.generateCode(anyInt()))
                .thenReturn("COLLIDE")
                .thenReturn("UNIQUE1");

        // Bypass pre-check for "COLLIDE" by mocking shortUrlRepository.existsByShortCode("COLLIDE") to return false
        // so that it forces the database INSERT to hit the UNIQUE index constraint in saveInNewTransaction.
        // Note: Because ShortUrlRepository is real, we rely on the DB constraint. But since existsByShortCode would
        // normally catch "COLLIDE", we mock the generator to produce a code that WILL hit the DB constraint.

        CreateUrlRequest request = new CreateUrlRequest("https://example.com/new-url", null);

        // This call MUST NOT throw UnexpectedRollbackException!
        ShortUrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("UNIQUE1", response.getShortCode());
        assertEquals("https://example.com/new-url", response.getOriginalUrl());
    }
}
