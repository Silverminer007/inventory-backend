# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Dev mode (auto-starts PostgreSQL via Dev Services, live reload)
./mvnw quarkus:dev

# Build
./mvnw package

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ItemResourceTest

# Run a specific test method
./mvnw test -Dtest=ItemResourceTest#testGetAllItemsEndpoint

# Start PostgreSQL manually (alternative to Dev Services)
docker-compose up -d postgres
```

Dev UI: `http://localhost:8080/q/dev` | Swagger UI: `http://localhost:8080/q/swagger-ui` | Health: `http://localhost:8080/q/health`

## Architecture

Quarkus 3.17.3 / Java 21 backend with PostgreSQL 16. Layered architecture under `de.henzeob.inventory`:

- **`api/`** — JAX-RS REST resources. Endpoints at `/api/v1/{resource}`. `GlobalExceptionMapper` handles centralized error responses.
- **`application/`** — Service layer with business logic and `@Transactional` management (`ItemService`, `ContainerService`, `TaggingService`, `AuditLogService`).
- **`model/entity/`** — JPA entities extending `PanacheEntityBase`. Unified `Container` entity with `containerType` (ROOM, SHELF, BOX) and self-referential `parentContainer` for nesting. Items reference a single `container` FK. All entities carry a `userId` field for tenant isolation.
- **`model/dto/`** — DTOs decoupled from entities for API contracts.
- **`mapper/`** — Entity ↔ DTO conversion.
- **`repository/`** — Panache repositories with user-scoped queries. Fuzzy search uses PostgreSQL `pg_trgm`.

## Database

- Flyway migrations in `src/main/resources/db/migration/` — auto-run at startup
- Entities use optimistic locking via `@Version` fields
- Tests use H2 in-memory database automatically (`quarkus-test-h2`)
- Local PostgreSQL via Docker Compose on port 5432 (credentials: `inventory/inventory/inventory`)

## Adding New Features

When adding a new entity/resource, create: Entity, DTO, Mapper, Repository, Service, Resource, Flyway migration, and tests. Follow the Item implementation as the reference pattern.

## Testing

Tests use `@QuarkusTest` + REST Assured. Located in `src/test/java/de/henzeob/inventory/`. Follow the pattern in `ItemResourceTest.java`.

## Key Details

- Auth is currently hardcoded to "demo-user" (Keycloak OIDC configured but disabled for MVP)
- CORS allows `localhost:3000` and `localhost:5173` (frontend dev servers)
- Audit logging tracks all entity changes (CREATE, UPDATE, DELETE, MOVE) per F71-F80
- Frontend is a separate Nuxt.js 3 PWA (not in this repo)
