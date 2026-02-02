# Inventory Backend

Backend-Service für das Lagerverwaltungssystem, gebaut mit Quarkus.

## Voraussetzungen

- Java 21
- Maven 3.9+
- Docker & Docker Compose (für lokale Entwicklung)

## Lokale Entwicklung

### 1. Datenbank starten

```bash
docker-compose up -d postgres
```

### 2. Anwendung im Dev-Mode starten

```bash
./mvnw quarkus:dev
```

Die Anwendung startet auf http://localhost:8080

**Dev-Mode Features:**
- Live Reload bei Code-Änderungen
- Dev UI: http://localhost:8080/q/dev
- Swagger UI: http://localhost:8080/q/swagger-ui
- Health Check: http://localhost:8080/q/health

### 3. API testen

#### Items abrufen
```bash
curl http://localhost:8080/api/v1/items
```

#### Item erstellen
```bash
curl -X POST http://localhost:8080/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "MacBook Pro 14\"",
    "roomId": 1,
    "quantity": 1
  }'
```

#### Items suchen
```bash
curl "http://localhost:8080/api/v1/items/search?q=laptop"
```

## Datenbank-Migrationen

Flyway läuft automatisch beim Start. Migrationen liegen in:
```
src/main/resources/db/migration/
```

Neue Migration erstellen:
```
src/main/resources/db/migration/V2__add_photos.sql
```

## Testen

```bash
# Unit Tests
./mvnw test

# Integration Tests
./mvnw verify
```

## Build für Production

### JVM Build
```bash
./mvnw package
docker build -f Dockerfile.jvm -t inventory-backend .
```

### Native Build (optional, für kleinere Images)
```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t inventory-backend-native .
```

## Deployment

### Docker Compose (Vollständiges Setup)

```bash
docker-compose up -d
```

### Kubernetes/Hetzner

TODO: Kubernetes Manifests erstellen

## Konfiguration

Haupt-Konfiguration: `src/main/resources/application.properties`

Umgebungsvariablen für Production:
```bash
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://your-db:5432/inventory
QUARKUS_DATASOURCE_USERNAME=inventory
QUARKUS_DATASOURCE_PASSWORD=<secure-password>
```

## API Dokumentation

OpenAPI Spec: http://localhost:8080/q/openapi
Swagger UI: http://localhost:8080/q/swagger-ui

## Projekt-Struktur

```
src/main/java/de/inventory/
├── api/              # REST Resources
├── model/
│   ├── entity/      # JPA Entities
│   └── dto/         # Data Transfer Objects
├── repository/      # Data Access
├── service/         # Business Logic
└── mapper/          # Entity <-> DTO Mapping

src/main/resources/
├── db/migration/    # Flyway SQL Scripts
└── application.properties
```

## Features (MVP)

✅ CRUD für Items, Boxes, Shelves, Rooms
✅ Hierarchische Standorte (Room > Shelf > Box > Item)
✅ Automatisches Tagging (Rule-based)
✅ Fuzzy-Search
✅ Audit Log
✅ OpenAPI Documentation

## TODO (Phase 2)

- [ ] Keycloak Integration
- [ ] WebSockets für Real-time Sync
- [ ] Photo Upload (S3/Hetzner Object Storage)
- [ ] QR-Code Generation
- [ ] Export (CSV, PDF)

## Troubleshooting

### Datenbank-Verbindung fehlgeschlagen
```bash
# Prüfe ob PostgreSQL läuft
docker-compose ps

# Logs anschauen
docker-compose logs postgres
```

### Flyway Migration fehlgeschlagen
```bash
# Flyway Status prüfen
./mvnw flyway:info

# Migration zurücksetzen (Vorsicht in Production!)
./mvnw flyway:clean
```

## Lizenz

Private Project - KjG Nutzung