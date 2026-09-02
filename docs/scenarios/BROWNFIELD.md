# Brownfield Integration Scenario

## Overview
This document outlines how this URL Shortener Service can be integrated into an existing enterprise ecosystem (Brownfield environment).

## Integration Strategies

### 1. Database Integration & Migrations
- For brownfield integration with existing database infrastructure, replace Spring JPA `ddl-auto: update` with **Flyway** or **Liquibase** migration scripts.
- Schema changes for existing DBs should be managed via versioned migration files in `src/main/resources/db/migration/`.

### 2. Authentication & Authorization (OAuth2 / JWT)
- Protect management endpoints (`POST /api/urls`, `GET /api/urls/{shortCode}/analytics`) by adding `spring-boot-starter-oauth2-resource-server`.
- Public redirect endpoint (`GET /{shortCode}`) remains unauthenticated for public access.

### 3. Distributed Caching (Redis Integration)
- High-volume redirects can be offloaded to Redis:
  - Cache key: `shortcode:{shortCode}` -> Value: `originalUrl`
  - On `GET /{shortCode}`, check Redis first before querying database.
  - Asynchronously queue click increment events using Redis Streams or Kafka to eliminate DB write bottleneck under multi-million request loads.
