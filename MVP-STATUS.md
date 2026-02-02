# 🎯 Inventory Backend MVP - Status & Nächste Schritte

## ✅ Was ist fertig?

### Projekt-Setup
- ✅ Maven Projekt mit Quarkus 3.17.3
- ✅ PostgreSQL Datenbank-Schema
- ✅ Flyway Migrations
- ✅ Docker Compose für lokale Entwicklung
- ✅ Dockerfile für Production Build

### Datenmodell
- ✅ Item Entity (Gegenstände)
- ✅ Box Entity (Kisten, mit Verschachtelung)
- ✅ Shelf Entity (Regale)
- ✅ Room Entity (Räume mit PERMANENT/TEMPORARY)
- ✅ Audit Log Tabelle
- ✅ Sync Events Tabelle (vorbereitet)
- ✅ Synonyme & Tag-Regeln Tabellen

### API Endpoints (Items)
- ✅ GET /api/v1/items - Alle Items
- ✅ GET /api/v1/items/{id} - Ein Item
- ✅ POST /api/v1/items - Neues Item
- ✅ PUT /api/v1/items/{id} - Item aktualisieren
- ✅ DELETE /api/v1/items/{id} - Item löschen
- ✅ POST /api/v1/items/{id}/move - Item verschieben
- ✅ GET /api/v1/items/search?q=... - Suche
- ✅ GET /api/v1/items/by-tag/{tag} - Nach Tag filtern

### Features
- ✅ Automatisches Tagging (Rule-based mit Keywords)
- ✅ Fuzzy-Search (PostgreSQL pg_trgm)
- ✅ Audit Log (automatisch bei allen Änderungen)
- ✅ Hierarchische Standorte (Room > Shelf > Box > Item)
- ✅ Optimistic Locking (Version-Feld)
- ✅ OpenAPI/Swagger Dokumentation
- ✅ Health Checks
- ✅ Global Exception Handler

### Code-Qualität
- ✅ Clean Architecture (API -> Service -> Repository -> Entity)
- ✅ DTO Pattern (Entity/DTO Trennung)
- ✅ Mapper Pattern
- ✅ Validation (@Valid, @NotNull, etc.)
- ✅ Basic Tests

## ⏳ Was fehlt noch für MVP?

### High Priority (für funktionstüchtiges MVP)

1. **Room/Shelf/Box Resources** (3-4 Stunden)
    - [ ] RoomResource + RoomService + RoomDTO + RoomMapper
    - [ ] ShelfResource + ShelfService + ShelfDTO + ShelfMapper
    - [ ] BoxResource + BoxService + BoxDTO + BoxMapper
    - Kannst du kopieren von ItemResource und anpassen!

2. **QR-Code Generierung** (1-2 Stunden)
    - [ ] QRCodeService mit ZXing Library
    - [ ] GET /api/v1/items/{id}/qr-code Endpoint
    - [ ] Analog für Boxes, Shelves, Rooms

3. **Synonym-Suche Integration** (1 Stunde)
    - [ ] SynonymService
    - [ ] Erweitere searchItems() um Synonym-Lookup

4. **Test-Daten Script** (30 Minuten)
    - [ ] SQL-Script mit Beispiel-Daten zum Testen
    - [ ] Oder: DataInitializer Bean für Dev-Mode

### Medium Priority (Nice-to-have für MVP)

5. **WebSocket Sync** (2-3 Stunden)
    - [ ] SyncWebSocket Endpoint
    - [ ] Event Broadcasting bei Änderungen

6. **Bessere Suche** (1-2 Stunden)
    - [ ] Multi-Field Search (Name + Description + Tags)
    - [ ] Ranking/Scoring
    - [ ] Faceted Search (nach Kategorien filtern)

7. **Pagination** (1 Stunde)
    - [ ] Paging für GET /items
    - [ ] Limit & Offset Parameter

### Low Priority (Phase 2)

8. **Keycloak Integration** (2-4 Stunden)
    - [ ] application.properties konfigurieren
    - [ ] @RolesAllowed auf Endpoints
    - [ ] getCurrentUserId() aus JWT Token

9. **Photo Upload** (3-4 Stunden)
    - [ ] S3Manager Service (Hetzner Object Storage)
    - [ ] PhotoResource
    - [ ] Multipart File Upload

10. **Export Funktionen** (2-3 Stunden)
    - [ ] CSV Export
    - [ ] PDF Export mit iText
    - [ ] Etiketten-Generator

## 📋 Implementierungs-Reihenfolge (Empfehlung)

### Phase 1: MVP fertigstellen (8-10 Stunden)
1. Room/Shelf/Box Resources (analog zu Items)
2. Test-Daten Script
3. QR-Code Generierung
4. Synonym-Suche
5. Manuelle Tests & Bugfixes

### Phase 2: Frontend-Integration vorbereiten (4-6 Stunden)
1. WebSocket Sync
2. Pagination
3. Bessere Suche
4. CORS richtig konfigurieren

### Phase 3: Production-Ready (6-8 Stunden)
1. Keycloak Integration
2. Deployment auf Hetzner
3. Monitoring & Logging
4. Backup-Strategie

### Phase 4: Advanced Features (nach Bedarf)
1. Photo Upload
2. Export Funktionen
3. ML-basiertes Tagging (wenn genug Daten)

## 🚀 Quick Wins (1-2 Stunden Arbeit, großer Impact)

### 1. Room Resource (30 Minuten)

```java
// api/RoomResource.java
@Path("/api/v1/rooms")
public class RoomResource {
    @Inject RoomService roomService;
    
    @GET
    public Response getAllRooms() {
        return Response.ok(roomService.getAllRooms(getUserId())).build();
    }
    
    @POST
    public Response createRoom(@Valid RoomDTO dto) {
        return Response.ok(roomService.createRoom(dto, getUserId())).build();
    }
    
    // etc - kopiere von ItemResource
}
```

### 2. Test-Daten (15 Minuten)

```sql
-- src/main/resources/db/migration/V2__test_data.sql
INSERT INTO rooms (name, location_type, user_id, last_modified) VALUES
('Keller', 'PERMANENT', 'demo-user', NOW()),
('Ferienfreizeit 2025', 'TEMPORARY', 'demo-user', NOW());

INSERT INTO shelves (name, room_id, user_id, last_modified) VALUES
('Regal A', 1, 'demo-user', NOW());

INSERT INTO boxes (name, shelf_id, user_id, last_modified) VALUES
('Kiste 1', 1, 'demo-user', NOW());

INSERT INTO items (name, box_id, quantity, user_id, last_modified) VALUES
('Hammer', 1, 1, 'demo-user', NOW()),
('Laptop', 1, 1, 'demo-user', NOW());
```

### 3. QR-Code Service (45 Minuten)

Dependency hinzufügen in pom.xml:
```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

Service erstellen:
```java
@ApplicationScoped
public class QRCodeService {
    public byte[] generateQRCode(String data) {
        // ZXing QR-Code Generierung
        // Return PNG als byte[]
    }
}
```

## 📊 Zeit-Schätzung

| Task | Zeit | Priorität |
|------|------|-----------|
| Room/Shelf/Box Resources | 3-4h | HIGH |
| Test-Daten | 0.5h | HIGH |
| QR-Codes | 1-2h | HIGH |
| Synonym-Suche | 1h | MEDIUM |
| WebSocket Sync | 2-3h | MEDIUM |
| Pagination | 1h | MEDIUM |
| Keycloak | 2-4h | LOW |
| Photos | 3-4h | LOW |
| **Total MVP** | **8-10h** | |
| **Total Production-Ready** | **18-25h** | |

## 🎓 Lern-Empfehlung

Wenn du mit Quarkus noch nicht so vertraut bist:

1. **Start simple**: Kopiere ItemResource für Room/Shelf/Box
2. **Ein Feature nach dem anderen**: Nicht alles gleichzeitig
3. **Teste häufig**: Nach jedem Feature testen
4. **Dev UI nutzen**: http://localhost:8080/q/dev ist sehr hilfreich
5. **Swagger UI**: Zum API-Testen ohne curl

## 🐛 Known Issues / TODOs

- [ ] getCurrentUserId() ist hardcoded "demo-user" - später mit Keycloak ersetzen
- [ ] Keine User-Isolation im Dev-Mode (alle sehen alle Items)
- [ ] Error Messages könnten besser sein
- [ ] Tests sind sehr basic
- [ ] Keine Pagination bei großen Datenmengen

## 🎯 Definition of Done (MVP)

MVP ist fertig wenn:
- [x] Items CRUD funktioniert ✅
- [ ] Rooms/Shelves/Boxes CRUD funktioniert
- [x] Suche funktioniert (Name + Fuzzy) ✅
- [ ] QR-Codes können generiert werden
- [x] Auto-Tagging funktioniert ✅
- [ ] Test-Daten vorhanden
- [ ] README mit Setup-Anleitung (✅)
- [ ] Manuell getestet (kompletter Workflow)

**Geschätzte Zeit bis MVP: 8-10 Stunden reine Coding-Zeit**

## 💡 Nächster Schritt

**Empfehlung:** Starte mit Room Resource!

```bash
cd inventory-backend
./mvnw quarkus:dev

# In neuem Terminal/Tab:
# Erstelle src/main/java/de/inventory/api/RoomResource.java
# Kopiere von ItemResource.java und passe an
```

Viel Erfolg! 🚀