package com.schwab.urlshortener.repository;

import com.schwab.urlshortener.entity.ShortUrl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShortUrlSaveHelper {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlSaveHelper(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortUrl saveInNewTransaction(ShortUrl shortUrl) {
        return shortUrlRepository.saveAndFlush(shortUrl);
    }
}
