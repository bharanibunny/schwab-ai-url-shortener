# Production URL Shortener Service

A production-grade, high-performance URL shortener service built with **Java 17**, **Spring Boot 3.3.4**, **Spring Data JPA**, **H2 / PostgreSQL**, **OpenAPI / Swagger**, and **Docker**.

## Features

- **Short URL Creation (`POST /api/urls`)**: Validates long URLs (`http`/`https` scheme), checks optional expiration timestamp, and generates a random 7-character Base62 string using `SecureRandom`.
- **Collision Retry Logic**: Gracefully detects short-code collisions and retries candidate generation up to a configurable limit (default: 5 retries).
- **HTTP 302 Redirection (`GET /{shortCode}`)**: Resolves target original URL, checks expiration (`410 Gone` if expired, `404 Not Found` if missing), and atomically increments click count.
- **Analytics Endpoint (`GET /api/urls/{shortCode}/analytics`)**: Exposes short code metadata, original URL, creation time, expiration time, and total click count.
- **Atomic Click Counter**: Uses a database-level query (`UPDATE short_url SET click_count = click_count + 1 WHERE short_code = :code`) to prevent lost updates under high concurrency.
- **OpenAPI / Swagger UI**: Built-in interactive documentation at `/swagger-ui.html`.

## Technology Stack

- **Java**: 17
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation)
- **Database**: H2 (In-memory for `dev`), PostgreSQL-ready (`prod`)
- **Documentation**: Springdoc OpenAPI (Swagger UI)
- **Testing**: JUnit 5, Mockito, Spring Boot MockMvc Integration Testing
- **Build Tool**: Apache Maven
- **Containerization**: Docker & Docker Compose

## Prerequisites

- Java 17 JDK
- Apache Maven 3.8+ (or bundled wrapper)
- Docker & Docker Compose (optional for containerized deployment)

## Getting Started

### 1. Run Locally (Dev Profile with H2)

```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`.
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **H2 Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:urlshortenerdb`, User: `sa`, Password: empty)

### 2. Run Tests

```bash
mvn clean test
```

### 3. Run with Docker Compose (PostgreSQL Production Setup)

```bash
docker-compose up --build
```

---

## API Usage Examples

### 1. Create a Short URL (Auto-Generated Short Code)

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/some/very/long/url",
    "expiresAt": "2026-12-31T23:59:59Z"
  }'
```

**Response (`201 Created`)**:
```json
{
  "shortCode": "aB7kP2x",
  "shortUrl": "http://localhost:8080/aB7kP2x",
  "originalUrl": "https://example.com/some/very/long/url",
  "createdAt": "2026-09-01T19:15:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "clickCount": 0
}
```

### 2. Create a Short URL with Custom Alias

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/profile",
    "customAlias": "my-profile"
  }'
```

**Response (`201 Created`)**:
```json
{
  "shortCode": "my-profile",
  "shortUrl": "http://localhost:8080/my-profile",
  "originalUrl": "https://example.com/profile",
  "createdAt": "2026-09-01T19:30:00Z",
  "expiresAt": null,
  "clickCount": 0
}
```

### 2. Redirect to Original URL

```bash
curl -i http://localhost:8080/aB7kP2x
```

**Response (`302 Found`)**:
```http
HTTP/1.1 302 Found
Location: https://example.com/some/very/long/url
```

### 3. Retrieve Analytics

```bash
curl http://localhost:8080/api/urls/aB7kP2x/analytics
```

**Response (`200 OK`)**:
```json
{
  "shortCode": "aB7kP2x",
  "originalUrl": "https://example.com/some/very/long/url",
  "createdAt": "2026-09-01T19:15:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "clickCount": 1
}
```

---

## Documentation Directory

More detailed architectural and scenario documentation can be found in `docs/`:
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design & layered modular monolith
- [docs/DECISIONS.md](docs/DECISIONS.md) - Architecture Decision Records (ADRs)
- [docs/AI_EXECUTION_LOG.md](docs/AI_EXECUTION_LOG.md) - Record of AI recommendations, engineer decisions, & validation
- [docs/scenarios/GREENFIELD.md](docs/scenarios/GREENFIELD.md) - Greenfield project setup rationale
- [docs/scenarios/BROWNFIELD.md](docs/scenarios/BROWNFIELD.md) - Strategies for integrating into existing enterprise systems
- [docs/scenarios/AMBIGUOUS.md](docs/scenarios/AMBIGUOUS.md) - Handling ambiguity and domain edge cases
