# Backend Setup - Schritt für Schritt

## 🚀 Schnellstart (5 Minuten)

### 1. Datenbank starten
```bash
cd inventory-backend
docker-compose up -d postgres
```

Warte bis PostgreSQL ready ist (ca. 10 Sekunden):
```bash
docker-compose logs -f postgres
# Warte auf: "database system is ready to accept connections"
```

### 2. Anwendung starten
```bash
./mvnw quarkus:dev
```

Beim ersten Start:
- Maven lädt alle Dependencies (~2-3 Minuten)
- Flyway erstellt Datenbank-Schema
- Quarkus startet im Dev-Mode

### 3. Testen

Öffne http://localhost:8080/q/dev in deinem Browser.

Oder teste die API:
```bash
# Health Check
curl http://localhost:8080/q/health

# Swagger UI im Browser öffnen
open http://localhost:8080/q/swagger-ui
```

## 📋 Projekt-Übersicht

### Was ist bereits implementiert?

✅ **Datenmodell**
- Items (Gegenstände)
- Boxes (Kisten)
- Shelves (Regale)
- Rooms (Räume)
- Hierarchische Beziehungen

✅ **REST API**
- CRUD für Items
- Suche (Name + Fuzzy-Search)
- Tag-basierte Suche
- Move Items

✅ **Features**
- Automatisches Tagging (Rule-based)
- Audit Log (alle Änderungen werden protokolliert)
- PostgreSQL mit Flyway Migrations
- OpenAPI/Swagger Dokumentation

### Was fehlt noch für MVP?

⏳ **Backend**
- [ ] CRUD für Boxes, Shelves, Rooms (analog zu Items)
- [ ] WebSocket für Real-time Sync
- [ ] QR-Code Generierung
- [ ] Synonym-Suche Integration

⏳ **Später (Phase 2)**
- [ ] Keycloak Integration
- [ ] Photo Upload (S3)
- [ ] Export Funktionen

## 🔧 Entwicklung

### Dev-Mode Features

Im Dev-Mode (`./mvnw quarkus:dev`) hast du:

1. **Live Reload** - Code-Änderungen werden automatisch neu geladen
2. **Dev UI** - http://localhost:8080/q/dev
    - Datenbank-Browser
    - Config Editor
    - Health Checks
    - und mehr

3. **Continuous Testing** - Drücke `r` in der Konsole für Tests

### Neue Endpoints hinzufügen

**Beispiel: Room Resource erstellen**

1. DTO erstellen (`model/dto/RoomDTO.java`)
2. Mapper erstellen (`mapper/RoomMapper.java`)
3. Service erstellen (`service/RoomService.java`)
4. Resource erstellen (`api/RoomResource.java`)

Orientiere dich an `ItemResource.java` als Vorlage!

### Datenbank-Migration hinzufügen

Neue Datei erstellen:
```bash
touch src/main/resources/db/migration/V2__add_qr_codes_table.sql
```

SQL schreiben:
```sql
CREATE TABLE qr_codes (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    code VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

Beim nächsten Start wird die Migration automatisch ausgeführt.

## 🧪 Testing

### Unit Tests ausführen
```bash
./mvnw test
```

### Integration Tests
```bash
./mvnw verify
```

### API manuell testen

**Beispiel-Workflow:**

1. **Room erstellen** (TODO: Endpoint noch nicht implementiert)
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Keller",
    "locationType": "PERMANENT"
  }'
```

2. **Item erstellen**
```bash
curl -X POST http://localhost:8080/api/v1/items \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "MacBook Pro 14 Zoll",
    "roomId": 1,
    "quantity": 1
  }'
```

3. **Items abrufen**
```bash
curl http://localhost:8080/api/v1/items
```

4. **Suchen**
```bash
curl "http://localhost:8080/api/v1/items/search?q=laptop"
```

## 🐛 Troubleshooting

### "Datenbank-Verbindung fehlgeschlagen"

**Lösung 1:** PostgreSQL Container prüfen
```bash
docker-compose ps
docker-compose logs postgres
```

**Lösung 2:** PostgreSQL neu starten
```bash
docker-compose restart postgres
```

**Lösung 3:** Port bereits belegt?
```bash
lsof -i :5432  # Zeigt was Port 5432 blockiert
```

### "Flyway Migration fehlgeschlagen"

**Ursache:** SQL-Fehler in Migration

**Lösung:**
```bash
# Migration-Status prüfen
./mvnw flyway:info

# In Development: Datenbank zurücksetzen
docker-compose down -v  # Löscht auch Volumes!
docker-compose up -d postgres
./mvnw quarkus:dev
```

### "Port 8080 already in use"

**Andere Anwendung auf Port 8080?**
```bash
lsof -i :8080
```

**Port ändern:**
```bash
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

### "OutOfMemoryError beim Build"

**Maven mehr RAM geben:**
```bash
export MAVEN_OPTS="-Xmx2g"
./mvnw clean package
```

## 📊 Nächste Schritte

### Schritt 1: Fehlende Entities implementieren (1-2 Stunden)

Kopiere die Item-Implementierung und passe an für:
- RoomResource + RoomService
- ShelfResource + ShelfService
- BoxResource + BoxService

### Schritt 2: Frontend anbinden (später)

Das Backend ist bereit für ein Frontend:
- CORS ist bereits konfiguriert
- Swagger UI zeigt alle Endpoints
- DTOs sind JSON-ready

### Schritt 3: Deployment vorbereiten

```bash
# Docker Image bauen
./mvnw package
docker build -t inventory-backend .

# Testen
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/inventory \
  inventory-backend
```

## 💡 Tipps

### IntelliJ IDEA Setup

1. Installiere Quarkus Plugin
2. Import als Maven Projekt
3. Enable Annotation Processing
4. Run Configuration: "Quarkus Dev Mode"

### VS Code Setup

1. Installiere Extensions:
    - Java Extension Pack
    - Quarkus Tools
2. Terminal: `./mvnw quarkus:dev`

### Datenbank-Browser

**DBeaver / DataGrip:**
- Host: localhost
- Port: 5432
- Database: inventory
- User: inventory
- Password: inventory

**pgAdmin (Web):**
```bash
docker run -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=admin@admin.com \
  -e PGADMIN_DEFAULT_PASSWORD=admin \
  dpage/pgadmin4
```

## 🎯 Checkliste für MVP-Fertigstellung

- [x] Items CRUD
- [x] Tagging System
- [x] Suche (Fuzzy)
- [x] Audit Log
- [ ] Boxes CRUD
- [ ] Shelves CRUD
- [ ] Rooms CRUD
- [ ] Move Items zwischen Containern
- [ ] QR-Code Generierung
- [ ] WebSocket Sync

Geschätzte Zeit für MVP-Fertigstellung: **4-6 Stunden** (wenn du die Item-Implementierung als Template nutzt)

## 📚 Weiterführende Ressourcen

- [Quarkus Guides](https://quarkus.io/guides/)
- [Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [REST Client](https://quarkus.io/guides/rest-client-reactive)
- [Testing Guide](https://quarkus.io/guides/getting-started-testing)