package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.ShortUrlResponse;

public interface UrlService {

    ShortUrlResponse createShortUrl(CreateUrlRequest request);

    String resolveAndRedirect(String shortCode);

    AnalyticsResponse getAnalytics(String shortCode);
}
