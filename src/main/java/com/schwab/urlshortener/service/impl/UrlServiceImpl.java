package com.schwab.urlshortener.service.impl;

import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.ShortUrlResponse;
import com.schwab.urlshortener.entity.ShortUrl;
import com.schwab.urlshortener.exception.AliasAlreadyExistsException;
import com.schwab.urlshortener.exception.CollisionExhaustionException;
import com.schwab.urlshortener.exception.UrlExpiredException;
import com.schwab.urlshortener.exception.UrlNotFoundException;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import com.schwab.urlshortener.repository.ShortUrlSaveHelper;
import com.schwab.urlshortener.service.UrlService;
import com.schwab.urlshortener.util.Base62ShortCodeGenerator;
import com.schwab.urlshortener.util.UrlValidatorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UrlServiceImpl implements UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final Base62ShortCodeGenerator codeGenerator;
    private final ShortUrlSaveHelper saveHelper;

    @Value("${app.short-code.max-retries:5}")
    private int maxRetries;

    @Value("${app.short-code.length:7}")
    private int codeLength;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlServiceImpl(ShortUrlRepository shortUrlRepository,
                          Base62ShortCodeGenerator codeGenerator,
                          ShortUrlSaveHelper saveHelper) {
        this.shortUrlRepository = shortUrlRepository;
        this.codeGenerator = codeGenerator;
        this.saveHelper = saveHelper;
    }

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(CreateUrlRequest request) {
        UrlValidatorUtil.validateUrl(request.getUrl());
        UrlValidatorUtil.validateExpiration(request.getExpiresAt());
        String normalizedAlias = UrlValidatorUtil.validateAndNormalizeCustomAlias(request.getCustomAlias());

        Instant now = Instant.now();
        ShortUrl savedEntity = null;

        if (normalizedAlias != null) {
            // Custom alias provided by caller
            if (shortUrlRepository.existsByShortCodeIgnoreCase(normalizedAlias)) {
                throw new AliasAlreadyExistsException(normalizedAlias);
            }
            ShortUrl shortUrl = new ShortUrl(normalizedAlias, request.getUrl().trim(), now, request.getExpiresAt());
            try {
                savedEntity = saveHelper.saveInNewTransaction(shortUrl);
            } catch (DataIntegrityViolationException e) {
                throw new AliasAlreadyExistsException(normalizedAlias);
            }
        } else {
            // Auto-generated short code path
            int attempts = 0;
            while (attempts < maxRetries) {
                attempts++;
                String candidateCode = codeGenerator.generateCode(codeLength);

                if (shortUrlRepository.existsByShortCode(candidateCode)) {
                    continue; // Collision detected via pre-check, retry
                }

                ShortUrl shortUrl = new ShortUrl(candidateCode, request.getUrl().trim(), now, request.getExpiresAt());
                try {
                    savedEntity = saveHelper.saveInNewTransaction(shortUrl);
                    break; // Successfully saved without collision
                } catch (DataIntegrityViolationException e) {
                    // Catch DB unique index constraint violation in isolated REQUIRES_NEW transaction
                    if (attempts >= maxRetries) {
                        throw new CollisionExhaustionException(maxRetries);
                    }
                }
            }
        }

        if (savedEntity == null) {
            throw new CollisionExhaustionException(maxRetries);
        }

        String fullShortUrl = baseUrl.endsWith("/") ? baseUrl + savedEntity.getShortCode() : baseUrl + "/" + savedEntity.getShortCode();
        return new ShortUrlResponse(
                savedEntity.getShortCode(),
                fullShortUrl,
                savedEntity.getOriginalUrl(),
                savedEntity.getCreatedAt(),
                savedEntity.getExpiresAt(),
                savedEntity.getClickCount()
        );
    }

    @Override
    @Transactional
    public String resolveAndRedirect(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .or(() -> shortUrlRepository.findByShortCodeIgnoreCase(shortCode))
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        Instant now = Instant.now();
        if (shortUrl.getExpiresAt() != null && !shortUrl.getExpiresAt().isAfter(now)) {
            throw new UrlExpiredException(shortCode);
        }

        // Atomically increment click count
        shortUrlRepository.incrementClickCount(shortUrl.getShortCode());

        return shortUrl.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .or(() -> shortUrlRepository.findByShortCodeIgnoreCase(shortCode))
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return new AnalyticsResponse(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getClickCount()
        );
    }

    // Setters for unit testing flexibility
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
