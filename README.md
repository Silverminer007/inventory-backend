# Inventory Backend

Quarkus REST API for managing physical inventory items across hierarchical containers.

## Features

- **Hierarchical containers** — ROOM → SHELF → BOX with arbitrary nesting and self-referential parent/child relationships
- **Full-text search** with Elasticsearch (German analyzer + synonym expansion via OpenThesaurus)
- **Event-sourced Command API** for offline-first / mobile sync with idempotent replay
- **Optimistic locking** with 3-way conflict detection and auto-merge (only true field-level conflicts require manual resolution)
- **S3 image management** — two-step upload (pre-upload to S3, then link via command)
- **LLM-powered auto-tagging** (Anthropic Claude) with database cache to avoid redundant API calls
- **Rule-based auto-tagging** fallback when LLM is disabled
- **Multi-tenant isolation** — all data scoped by `userId`

## Tech Stack

| Component | Version |
|-----------|---------|
| Quarkus | 3.17.3 |
| Java | 21 |
| PostgreSQL | 16 |
| Elasticsearch | 9.3.1 |
| S3 (Hetzner / MinIO) | — |
| Hibernate Search | via Quarkus BOM |
| Flyway | via Quarkus BOM |
| Panache | via Quarkus BOM |
| REST Assured (tests) | via Quarkus BOM |

## Prerequisites

- Java 21+
- Maven (wrapper included — no separate install needed)
- Docker + Docker Compose

## Quick Start

```bash
# 1. Start all infrastructure (PostgreSQL, MinIO, Elasticsearch, Kibana)
docker-compose up -d

# 2. Run in dev mode (live reload, Flyway migrations applied automatically)
./mvnw quarkus:dev
```

| UI | URL |
|----|-----|
| Swagger UI | http://localhost:8080/q/swagger-ui |
| Dev UI | http://localhost:8080/q/dev |
| Health | http://localhost:8080/q/health |
| MinIO Console | http://localhost:9001 |
| Kibana | http://localhost:5601 |

## Configuration

All settings have sensible local defaults. Override via environment variables for production.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/inventory` | JDBC URL |
| `DB_USER` | `inventory` | Database user |
| `DB_PASSWORD` | `inventory` | Database password |
| `S3_ENDPOINT` | `http://localhost:9000` | S3 / MinIO endpoint |
| `S3_ACCESS_KEY` | `minioadmin` | S3 access key |
| `S3_SECRET_KEY` | `minioadmin` | S3 secret key |
| `S3_BUCKET_NAME` | `inventory-images` | S3 bucket for images |
| `ELASTICSEARCH_HOSTS` | `localhost:9200` | Elasticsearch host:port |
| `ANTHROPIC_API_KEY` | *(empty)* | Optional — enables LLM tagging |
| `LLM_TAGGING` | `true` | Toggle LLM tagging on/off |

## Build & Test

```bash
# Build JAR
./mvnw package

# Run all tests (H2 in-memory, no Docker or Elasticsearch needed)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ItemResourceTest

# Run a specific test method
./mvnw test -Dtest=ItemResourceTest#testGetAllItemsEndpoint
```

## Architecture

```
HTTP Request
    │
    ▼
api/  (JAX-RS Resources)
    │   GlobalExceptionMapper centralises error responses
    ▼
application/  (Services + Handlers)
    │   @Transactional business logic
    │   CommandService dispatches to ItemCommandHandler / ContainerCommandHandler / …
    ▼
repository/  (Panache Repositories)
    │   User-scoped queries, findByIdAndUser helpers
    ▼
model/  (entity / dto / mapper)
    │   JPA entities extend PanacheEntityBase with @Version for optimistic locking
    │   DTOs decouple API contract from persistence
    └── mapper/  (entity ↔ DTO conversion)
```

### Package overview

| Package | Contents |
|---------|----------|
| `api` | JAX-RS REST resources, `GlobalExceptionMapper` |
| `application` | Service layer (`ItemService`, `ContainerService`, `CommandService`, …) |
| `application/handler` | Per-entity command handlers, `ConflictResult` sealed type |
| `model/entity` | JPA entities (`Item`, `Container`, `Command`, `Image`, `Synonym`, …) |
| `model/dto` | Request/response DTOs |
| `model/enums` | `CommandType`, `CommandStatus` |
| `mapper` | Entity ↔ DTO converters |
| `repository` | Panache repositories with user-scoped finders |
| `search` | Elasticsearch analysis configurer (German analyzer, synonyms) |

### Key design patterns

- **Panache repositories** — `findByIdAndUser` helpers enforce tenant isolation everywhere
- **`@Transactional` services** — all mutations go through a service method; resources stay thin
- **Command pattern** — write operations arrive as commands; the command log enables sync and audit
- **Optimistic locking** — `@Version` on `Item` and `Container`; stale writes trigger 3-way merge or CONFLICT

## Database

Flyway migrations run automatically at startup from `src/main/resources/db/migration/`.

| Migration | Description |
|-----------|-------------|
| V1 | Initial schema — items, containers, images, synonyms |
| V2 | Merge room/shelf/box into unified `containers` table with `container_type` |
| V3 | pg_trgm indexes for synonym search |
| V4 | Image support for containers (in addition to items) |
| V5 | `tag_suggestion_cache` table for LLM result caching |
| V6 | `item_tags` entity with `tag_type` (LLM / RULE / MANUAL) |
| V7 | `commands` table for event-sourced command log |
| V8 | Drop `audit_log` table (replaced by command history) |

### Key tables

| Table | Purpose |
|-------|---------|
| `items` | Inventory items with `@Version`, `container_id` FK, `user_id` |
| `containers` | Hierarchical containers; `parent_container_id` self-referential FK |
| `item_tags` | Tags associated with items (type: LLM / RULE / MANUAL) |
| `images` | S3 image metadata linked to items or containers |
| `commands` | Append-only command log (PENDING → APPLIED / FAILED) |
| `synonyms` | Search synonyms (auto-imported from OpenThesaurus) |
| `tag_suggestion_cache` | LLM tag suggestions keyed by item name |

## REST API Reference

All resource endpoints require no authentication in the current MVP (hardcoded to `demo-user`).

### Items — `/api/v1/items`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | All items for current user |
| `GET` | `/{id}` | Single item by ID |
| `GET` | `/search?q=&tags=` | Full-text search (Elasticsearch) with optional tag filter |
| `GET` | `/tags?q=` | Distinct tags, optionally filtered by prefix |
| `GET` | `/tags/suggest?item=` | LLM / rule-based tag suggestions for an item name |
| `GET` | `/by-tag/{tag}` | Items with a specific tag |

### Containers — `/api/v1/containers`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | All containers for current user |
| `GET` | `/roots` | Top-level containers (no parent) |
| `GET` | `/{id}` | Single container by ID |
| `GET` | `/{id}/children` | Direct children of a container |

### Images — `/api/v1/images`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/items/{id}` | Images for an item |
| `GET` | `/containers/{id}` | Images for a container |

### Image upload (step 1) — `/api/v1/images/upload`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Multipart upload — returns `s3Key` for use in `IMAGE_UPLOAD` command |

### Synonyms — `/api/v1/synonyms`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | All synonyms |

### Commands — `/commands`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Submit a batch of commands (array) |
| `GET` | `/?since=<ISO-8601>` | Fetch applied commands since timestamp (for sync) |

---

## Command API (Offline Sync)

The command API is the **single write path** for all mutations. Clients submit an array of commands; each is applied atomically in its own `REQUIRES_NEW` transaction. Commands are idempotent — resubmitting a `commandId` that was already `APPLIED` returns the cached result.

### CommandDTO

| Field | Type | Description |
|-------|------|-------------|
| `commandId` | UUID | Client-generated idempotency key (auto-assigned if omitted) |
| `commandType` | String | See command types below |
| `payloadVersion` | Integer | Always `1` |
| `entityId` | Long | Required for UPDATE / DELETE / MOVE commands |
| `payload` | Object | Command-specific fields (see below) |
| `clientId` | String | Optional — identifies the originating device |
| `clientSequence` | Long | Optional — monotonic counter per client |
| `issuedAt` | Instant | Optional — ISO-8601 timestamp of when client created the command |

### Command Types and Payloads

#### Item commands

| Type | Required payload fields | Optional payload fields |
|------|------------------------|------------------------|
| `ITEM_CREATE` | `name`, `containerId` | `description`, `position`, `quantity`, `barcode`, `tags` |
| `ITEM_UPDATE` | — | `name`, `description`, `position`, `quantity`, `barcode`, `tags`, `version`, `force` |
| `ITEM_DELETE` | — | `version`, `force` |
| `ITEM_MOVE` | `containerId` | `version`, `force` |

#### Container commands

| Type | Required payload fields | Optional payload fields |
|------|------------------------|------------------------|
| `CONTAINER_CREATE` | `name`, `containerType` (`ROOM`/`SHELF`/`BOX`) | `description`, `position`, `locationType`, `location`, `parentContainerId` |
| `CONTAINER_UPDATE` | — | `name`, `description`, `position`, `locationType`, `location`, `version`, `force` |
| `CONTAINER_DELETE` | — | `version`, `force` |
| `CONTAINER_MOVE` | `newParentContainerId` | `version`, `force` |

#### Image commands

| Type | Required payload fields | Optional payload fields |
|------|------------------------|------------------------|
| `IMAGE_UPLOAD` | `s3Key`, `entityId`, `entityType` (`ITEM`/`CONTAINER`) | `filename`, `contentType`, `fileSize` |
| `IMAGE_DELETE` | `imageId` | — |
| `IMAGE_SET_PRIMARY` | `imageId` | — |

#### Synonym commands

| Type | Required payload fields |
|------|------------------------|
| `SYNONYM_CREATE` | `word`, `synonyms` (array of strings) |
| `SYNONYM_DELETE` | `synonymId` |

### CommandResultDTO

```json
{
  "commandId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "APPLIED",
  "entityId": 42,
  "entityType": "ITEM",
  "serverSequence": 1337,
  "appliedAt": "2026-03-17T10:15:30Z",
  "snapshot": { "id": 42, "name": "Laptop", "version": 3 },
  "error": null,
  "conflictInfo": null
}
```

| Field | Description |
|-------|-------------|
| `status` | `APPLIED`, `FAILED`, or `CONFLICT` |
| `serverSequence` | Auto-increment ID from the `commands` table — use for ordered sync |
| `snapshot` | Current state of the entity after the command was applied |
| `conflictInfo` | Populated only when `status` is `CONFLICT` (see below) |

---

## Conflict Detection and Auto-Merge

When a client goes offline, modifies entities, and reconnects, the server performs a **3-way merge** to resolve concurrent modifications.

### Three tiers

| Scenario | Behaviour |
|----------|-----------|
| No `version` in payload | Legacy / force mode — applied immediately (no check) |
| `version` matches server | No concurrent modification — applied immediately |
| `version` is stale (`server.version > client.version`) | 3-way merge (see below) |

### 3-way merge logic

When a stale version is detected, the server:

1. Inspects the command history to determine which fields the **server** changed between `clientVersion` and the current version.
2. Compares each field the **client** wants to change against the server's current value.
3. A field is a **true conflict** only when **both** the client and server changed it to different values.

If no true conflicts exist → **auto-merge**: the client's changes are applied on top of the current server state (`APPLIED`).

If any true conflicts exist → **CONFLICT** response with full metadata for manual resolution.

### Force override

Include `"force": true` in the payload to skip all version checks and apply unconditionally.

### DELETE / MOVE with stale version

Any stale version on a DELETE or MOVE is always a CONFLICT (no field-level analysis — silently deleting or moving a modified entity risks data loss). Use `"force": true` to override.

### ConflictInfo shape

```json
{
  "conflictInfo": {
    "clientVersion": 5,
    "serverVersion": 7,
    "conflictingFields": ["name"],
    "serverSnapshot": { "id": 42, "name": "Server Name", "version": 7 },
    "clientPayload": { "name": "Client Name", "version": 5 }
  }
}
```

| Field | Description |
|-------|-------------|
| `clientVersion` | Version the client based its changes on |
| `serverVersion` | Current server version |
| `conflictingFields` | Fields where both sides made different changes; empty for DELETE/MOVE conflicts |
| `serverSnapshot` | Full current state of the entity — client can show this to the user |
| `clientPayload` | The payload the client submitted — client can diff against `serverSnapshot` |

CONFLICT commands are **not persisted** — no state changes occurred, so retrying re-evaluates freshly.

---

## Image Upload Flow

Images use a two-step flow to decouple binary upload from the command log.

**Step 1 — upload binary to S3:**

```
POST /api/v1/images/upload   (multipart/form-data, field: "file")

Response:
{
  "s3Key": "temp/demo-user/abc123.jpg",
  "s3Url": "http://localhost:9000/inventory-images/temp/demo-user/abc123.jpg",
  "filename": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 204800
}
```

**Step 2 — link to entity via command:**

```json
POST /commands
[{
  "commandId": "...",
  "commandType": "IMAGE_UPLOAD",
  "payload": {
    "s3Key": "temp/demo-user/abc123.jpg",
    "entityId": 42,
    "entityType": "ITEM",
    "filename": "photo.jpg",
    "contentType": "image/jpeg",
    "fileSize": 204800
  }
}]
```

---

## Testing

Tests use `@QuarkusTest` + REST Assured against an H2 in-memory database. No Docker or external services are needed.

```bash
./mvnw test
```

- **H2 in-memory** — Flyway migrations run on H2 at test startup; Elasticsearch is disabled in the test profile
- **Mock beans** — S3 and other external clients are replaced with `@Mock @ApplicationScoped` beans in test scope
- **Pattern** — follow `ItemResourceTest.java` as the reference: create entities, assert state, clean up within `@BeforeEach` / `@AfterEach`
