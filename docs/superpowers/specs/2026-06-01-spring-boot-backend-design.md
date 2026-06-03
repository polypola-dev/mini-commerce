# Mini Commerce Spring Boot Backend Spec

## Goal
Build a Spring Boot backend for the Mini Commerce portfolio MVP. This backend will handle product management, order processing, and Redis-based inventory concurrency.

## Requirements
- Spring Boot application using Gradle.
- Persistence: PostgreSQL via Spring Data JPA.
- Inventory Concurrency: Redis via Spring Data Redis.
- Validation: Spring Boot Starter Validation.
- Developer Experience: Lombok.

## Architecture
- `backend/build.gradle`: Project dependencies and configuration.
- `backend/src/main/resources/application.yml`: Database and Redis connection settings.
- `backend/src/main/java/com/minicommerce/MiniCommerceApplication.java`: Application entry point.

## Tech Stack
- Java 17+
- Spring Boot 3.x
- Gradle
- PostgreSQL
- Redis
- Lombok
- Spring Data JPA
- Spring Data Redis
