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

Quarkus 3.32.1 / Java 21 backend with PostgreSQL. This is a **command-sourced** backend: `DOMAIN-RULES.md` is the authoritative spec for domain behavior and is more current than this file for anything at the command/validation level — consult it first when in doubt. Commands are the sole source of truth; entity tables (`items`, `containers`, `categories`, `images`) are an incrementally-maintained snapshot, not independent state.

Layered architecture under `de.henzeob.inventory`:

- **`api/`** — `SyncResource` (`/api/v2/sync/fetchCommands`, `/api/v2/sync/applyCommands`) is the only way to mutate or read domain data. `ImageUploadResource` (`/api/v2/uploadImage/{id}`, `/api/v2/images/{id}`) is a thin S3 proxy for binary image data, keyed by an id already established via an `ITEM_IMAGE_CREATE`/`CONTAINER_IMAGE_CREATE` command. `GlobalExceptionMapper` centralizes error responses (400/404/409).
- **`application/`** — `SyncService` owns the doubly-linked command chain (fetch/apply, head validation, 409 on stale head). `CommandService` validates the command envelope (id, command_type, command_version) and dispatches to a handler. `application/handler/` holds one handler per entity (`ItemCommandHandler`, `ContainerCommandHandler`, `CategoryCommandHandler`, `ItemImageCommandHandler`, `ContainerImageCommandHandler`) — each does strict payload field validation (`PayloadValidator`, unknown fields anywhere in a payload reject the whole command) and calls the corresponding `*Service` for persistence and cross-entity checks (existence, delete guards, circular-reference checks). `ImageCompressor` re-encodes uploads as JPEG to fit the 500KB target.
- **`model/entity/`** — Plain `PanacheEntityBase` entities with public fields. `Container.ROOT_ID` / `Command.ROOT_COMMAND_ID` are the fixed UUID `11111111-1111-1111-1111-111111111111` seeded once by migration; that container/command can't be edited or deleted through the API. No `userId` — this is single-tenant (the whole command chain is global, matching the hardcoded "demo-user" auth).
- **`model/dto/`** — `CommandDTO` (`id`/`command_type`/`command_version`/`payload`) and `CommandEntryDTO` (`parent`/`child`/`command`) are the only wire types; the wrapper carries the linked-list bookkeeping so the command itself matches the shape in `DOMAIN-RULES.md` exactly.
- **`repository/`** — thin Panache repositories, mostly existence-check queries (`existsByContainer`, `existsByCategory`, `existsByPrimaryImage`, etc.) used by the delete guards.

## Database

- Flyway migrations in `src/main/resources/db/migration/` — auto-run at startup. `V14__new_commands.sql` is the full current schema (categories/containers/items/images/commands) and seeds the ROOT container + command; migrations before it are historical leftovers from a prior CRUD-based iteration of this app and mostly get dropped/superseded by V14.
- Tests run against a real PostgreSQL via Quarkus Dev Services (Testcontainers) — **not H2**, there is no H2 dependency in this project. Dev Services needs a working Docker/Podman socket; if `DOCKER_HOST` isn't already exported for podman, tests will fail to start a container.

## Testing

Tests use `@QuarkusTest` + REST Assured, driving the real `/api/v2/sync/*` endpoints end-to-end (see `support/SyncTestSupport.java`). Because the command chain is one global, append-only, never-reset log for the whole test session, every test must fetch the current head itself (`currentHead()`) rather than assuming a known starting state, and must use fresh random UUIDs for entities it creates. Test classes are organized to mirror the "Test Cases" sections of `DOMAIN-RULES.md` (`ItemCommandTest`, `ContainerCommandTest`, `CategoryCommandTest`, `ItemImageCommandTest`, `ContainerImageCommandTest`, `MixedCommandTest`, `CommandValidationTest`, `SyncTest`).

## Key Details

- Auth is currently hardcoded to "demo-user" (Keycloak OIDC configured but disabled for MVP); there is no per-user data isolation at the command/entity level.
- Frontend is a separate Nuxt.js 3 PWA (not in this repo).
- Command payload field names are snake_case exactly as written in `DOMAIN-RULES.md` (`container`, `parent`, `created_at`, `primary_image`, `shortcode`, `category`, `type`) — not camelCase.
- A command is rejected outright if its payload contains any field outside that command_type's allowed set (this is how e.g. a stray `created_at` on an `*_UPDATE` fails even though the value itself would be valid).
