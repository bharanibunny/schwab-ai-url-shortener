# System Architecture & Design

## Overview
The URL Shortener Service is implemented as a **Modular Monolith** prioritizing simplicity, high throughput, zero lost updates under concurrency, and clear separation of concerns.

## Layered Design

```
+-------------------------------------------------------+
|                   Controller Layer                    |
|  - UrlController (POST /api/urls, GET analytics)       |
|  - RedirectController (GET /{shortCode})              |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                    Service Layer                      |
|  - UrlServiceImpl (Validation, Collision Loop)        |
|  - Base62ShortCodeGenerator (SecureRandom)           |
|  - UrlValidatorUtil (Syntax & Scheme validation)      |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                   Repository Layer                    |
|  - ShortUrlRepository (Spring Data JPA)              |
|  - Atomic Update: UPDATE short_url SET click_count+1  |
+-------------------------------------------------------+
                           |
                           v
+-------------------------------------------------------+
|                   Database Layer                      |
|  - ShortUrl Entity (UNIQUE Index on short_code)       |
|  - H2 (Dev) / PostgreSQL (Prod)                       |
+-------------------------------------------------------+
```

## Key Components

### 1. Short Code Generation
- Alphabet: `A-Z`, `a-z`, `0-9` (Base62 = 62 characters).
- Length: 7 characters ($62^7 \approx 3.52 \times 10^{12}$ combinations).
- Generator: Uses `java.security.SecureRandom` to prevent enumeration and collision predictability.
- Collision Protection: Double layer. Service-level pre-check (`existsByShortCode`) + database-level `UNIQUE` constraint fallback. Collision retry limit capped at 5.

### 2. High Concurrency Click Incrementing
- Standard JPA dirty checking (`entity.setClickCount(...)` and `save(...)`) causes dirty read race conditions when multiple users hit `GET /{shortCode}` simultaneously.
- Solution: Atomic query execution:
  ```sql
  UPDATE short_url SET click_count = click_count + 1 WHERE short_code = :shortCode
  ```

### 3. Expiration Management
- `expiresAt` is stored as an ISO-8601 UTC timestamp (`TIMESTAMP WITH TIME ZONE`).
- Checked dynamically on redirect (`GET /{shortCode}`).
- Expired links return `410 Gone` on redirect attempts.
- Analytics endpoints return `200 OK` with metrics to allow historical analysis.
