package com.schwab.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class CreateUrlRequest {

    @NotBlank(message = "url must not be blank")
    private String url;

    private Instant expiresAt;
    private String customAlias;

    public CreateUrlRequest() {
    }

    public CreateUrlRequest(String url, Instant expiresAt) {
        this(url, expiresAt, null);
    }

    public CreateUrlRequest(String url, Instant expiresAt, String customAlias) {
        this.url = url;
        this.expiresAt = expiresAt;
        this.customAlias = customAlias;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }
}
