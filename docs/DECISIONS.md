# Architecture Decision Records (ADRs)

## ADR-001: Modular Monolith vs Microservices
- **Context**: Initial prototype needed for URL Shortener service.
- **Decision**: Use a single Spring Boot modular monolith without microservice overhead or message brokers.
- **Status**: ACCEPTED.
- **Rationale**: Minimal operational overhead, simpler testing, faster initial development while remaining modular for future extraction.

## ADR-002: Base62 Base Random Generation with SecureRandom
- **Context**: Short code generation must be non-predictable to prevent URL enumeration.
- **Decision**: Use `SecureRandom` over Base62-encoded sequential auto-increment IDs.
- **Status**: ACCEPTED.
- **Rationale**: Sequential auto-increment IDs reveal total link counts and enable bulk scraping of private short links.

## ADR-003: Atomic Database Increment Query for Click Counts
- **Context**: High concurrent redirect traffic can cause lost updates when updating `clickCount`.
- **Decision**: Implement `@Modifying @Query` database update (`UPDATE short_url SET click_count = click_count + 1 WHERE short_code = :shortCode`).
- **Status**: ACCEPTED.
- **Rationale**: Guarantees atomic update at the DB level, avoiding locking issues or stale entity overwrites.

## ADR-004: HTTP 410 Gone for Expired Short URLs
- **Context**: Short URLs can optionally expire.
- **Decision**: Return HTTP 410 Gone for expired link redirects and HTTP 404 Not Found for non-existent codes.
- **Status**: ACCEPTED.
- **Rationale**: Standard HTTP semantics clearly differentiate missing resources (404) from permanently expired resources (410).

## ADR-005: Expiration Micro-Window Timing Race Acceptance
- **Context**: Requests arriving at the exact millisecond of link expiration could execute read and increment without pessimistic database locking.
- **Decision**: Reject pessimistic database locking and accept the boundary timing condition as low-risk.
- **Status**: REJECTED (Pessimistic Locking).
- **Rationale**: Database locking introduces severe throughput bottlenecks and contention under high concurrency, which is unjustified for an edge-case timing race on expiration boundaries.

## ADR-006: Rate Limiting Delegated to Proxy/Gateway Tier
- **Context**: Abuse prevention requires rate limiting on creation and redirect endpoints.
- **Decision**: Defer application-level distributed rate-limiting libraries (e.g. Bucket4j/Redis) and document as a production gateway requirement.
- **Status**: DEFERRED (Application-level dependency).
- **Rationale**: Rate limiting is best enforced at the ingress layer (Nginx, Kong, Spring Cloud Gateway) to keep prototype application logic focused and free of extra stateful infrastructure.

## ADR-007: Non-Fetching HTTP 302 Redirect & SSRF Mitigation
- **Context**: Short URLs can target any HTTP/HTTPS address, including internal IP ranges.
- **Decision**: Service performs standard HTTP 302 client redirection without server-side HTTP fetching. IP blacklisting is deferred to production network egress policies.
- **Status**: DEFERRED (IP Blacklisting).
- **Rationale**: Traditional server-side SSRF does not occur because the backend never fetches destination content. Phishing/internal abuse concerns are governed at the enterprise proxy level.

## ADR-008: Single-Column Storage for Custom Aliases & Constraint Definition
- **Context**: Users can optionally supply a custom alias (`customAlias`) or receive an auto-generated 7-character Base62 code.
- **Decision**: Store both custom aliases and generated short codes in the single `short_code` database column. Expand column size from `VARCHAR(7)` to `VARCHAR(30)`. Enforce custom alias constraints: length 4–30 chars, allowed chars `a-z0-9_-`, lowercasing normalization, leading/trailing symbol protection, system reserved keyword blocking, and `HTTP 409 Conflict` on duplicate collisions.
- **Status**: ACCEPTED.
- **Rationale**: Avoids dual-column index overhead and complex `OR` database query logic. Engineer modified AI recommendation (4-30 chars instead of 3-32) to prevent short alias pollution and maintain clean database bounds.
