# Greenfield Scenario Execution

## Overview
When building a project from scratch (Greenfield), established patterns and boilerplate must be deliberately established to ensure long-term code quality, security, and developer efficiency.

## Actions Taken
1. **Clean Project Structure**: Created a modular monolith package structure separating controllers, services, repositories, entities, DTOs, utilities, and exception handlers.
2. **Explicit Dependency Selection**: Standardized on Spring Boot 3.3.4, Java 17, Spring Data JPA, Spring Validation, OpenAPI (Springdoc), and JUnit 5 / Mockito.
3. **Database Setup**: Selected H2 in-memory for zero-friction local development and PostgreSQL-ready dialect configuration for production deployments.
4. **Configuration Profiles**: Created isolated `dev` and `prod` application YAML profiles.
