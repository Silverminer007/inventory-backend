# Architekturentwurf: Lagerverwaltungssystem

## 1. Systemüberblick

### 1.1 Architekturstil
**Progressive Web Application (PWA)** mit **Client-Server-Architektur**

- **Frontend**: Nuxt.js 3 PWA (läuft im Browser, installierbar auf mobilen Geräten)
- **Backend**: Quarkus REST API
- **Datenbank**: PostgreSQL
- **Auth**: Keycloak
- **Hosting**: AWS (alternativ: Hetzner für Cost-Optimization)

### 1.2 Architektur-Prinzipien
1. **Offline First**: System muss auch ohne Internet voll funktionsfähig sein
2. **Mobile First**: UI primär für mobile Nutzung optimiert
3. **API First**: Backend als saubere REST API
4. **Event-Driven**: Änderungen als Events für bessere Synchronisation
5. **KISS**: Einfache Lösungen bevorzugen, Komplexität vermeiden

---

## 2. System-Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Layer                             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Nuxt.js 3 PWA                              │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │ │
│  │  │   Pages  │  │ Components│  │  Stores  │             │ │
│  │  │  (Views) │  │   (UI)    │  │  (Pinia) │             │ │
│  │  └──────────┘  └──────────┘  └──────────┘             │ │
│  │  ┌──────────────────────────────────────┐              │ │
│  │  │      Service Worker (Offline)        │              │ │
│  │  └──────────────────────────────────────┘              │ │
│  │  ┌──────────────────────────────────────┐              │ │
│  │  │   IndexedDB (Local Storage)          │              │ │
│  │  └──────────────────────────────────────┘              │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS / WebSocket
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway Layer                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          Application Load Balancer (AWS ALB)           │ │
│  │              + WAF (Optional für Security)              │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│   Keycloak Container     │  │   Backend Container       │
│   (Authentication)       │  │   (Quarkus Application)   │
│                          │  │                           │
│  - User Management       │  │  ┌────────────────────┐  │
│  - OAuth2/OIDC          │  │  │  REST API Layer    │  │
│  - JWT Token Issue      │  │  │  - Items API       │  │
│                          │  │  │  - Storage API     │  │
└──────────────────────────┘  │  │  - Sync API        │  │
                              │  └────────────────────┘  │
                              │  ┌────────────────────┐  │
                              │  │  Service Layer     │  │
                              │  │  - Business Logic  │  │
                              │  └────────────────────┘  │
                              │  ┌────────────────────┐  │
                              │  │  Repository Layer  │  │
                              │  │  - Data Access     │  │
                              │  └────────────────────┘  │
                              └──────────────────────────┘
                                          │
                                          ▼
                              ┌──────────────────────────┐
                              │   PostgreSQL Database    │
                              │   (RDS Multi-AZ)         │
                              │                          │
                              │  - Inventory Data        │
                              │  - User Data             │
                              │  - Sync Events           │
                              │  - Audit Log             │
                              └──────────────────────────┘
                                          │
                                          ▼
                              ┌──────────────────────────┐
                              │   S3 Bucket              │
                              │   (File Storage)         │
                              │                          │
                              │  - Photos                │
                              │  - QR Codes              │
                              │  - Exports               │
                              └──────────────────────────┘
```

---

## 3. Detaillierte Komponenten-Beschreibung

### 3.1 Frontend (Nuxt.js 3 PWA)

#### Technologie-Stack
```json
{
  "core": "Nuxt 3.x + TypeScript",
  "ui-framework": "Vuetify 3 (Material Design)",
  "state-management": "Pinia",
  "offline-storage": "IndexedDB (via Dexie.js)",
  "pwa": "@vite-pwa/nuxt",
  "http-client": "ofetch (Nuxt built-in)",
  "camera": "html5-qrcode",
  "image-handling": "browser-image-compression"
}
```

#### Verzeichnisstruktur
```
frontend/
├── assets/
│   ├── styles/
│   └── icons/
├── components/
│   ├── items/
│   │   ├── ItemCard.vue
│   │   ├── ItemList.vue
│   │   └── ItemSearch.vue
│   ├── storage/
│   │   ├── StorageHierarchy.vue
│   │   ├── LocationPicker.vue
│   │   └── ContainerCard.vue
│   ├── scanner/
│   │   ├── QRScanner.vue
│   │   └── BarcodeScanner.vue
│   └── common/
│       ├── OfflineIndicator.vue
│       └── SyncStatus.vue
├── composables/
│   ├── useOfflineSync.ts
│   ├── useCamera.ts
│   └── useIndexedDB.ts
├── pages/
│   ├── index.vue
│   ├── items/
│   │   ├── [id].vue
│   │   └── search.vue
│   ├── storage/
│   │   ├── rooms/[id].vue
│   │   ├── shelves/[id].vue
│   │   └── boxes/[id].vue
│   ├── scan.vue
│   └── history.vue
├── stores/
│   ├── items.ts
│   ├── storage.ts
│   ├── sync.ts
│   └── auth.ts
├── services/
│   ├── api/
│   │   ├── items.ts
│   │   ├── storage.ts
│   │   └── sync.ts
│   ├── offline/
│   │   ├── database.ts
│   │   ├── sync-queue.ts
│   │   └── conflict-resolver.ts
│   └── scanner/
│       └── qr-handler.ts
└── plugins/
    ├── indexeddb.client.ts
    └── pwa.client.ts
```

#### Offline-Strategie

**Service Worker Caching:**
```javascript
// Strategie: Network First mit Fallback
- API Calls: Network First (versuche Server, falle zurück auf Cache)
- Static Assets: Cache First (schnelles Laden)
- Bilder: Cache First mit lazy loading
```

**IndexedDB Schema:**
```typescript
// Dexie.js Datenbank-Schema
class InventoryDB extends Dexie {
  items: Table<Item>;
  boxes: Table<Box>;
  shelves: Table<Shelf>;
  rooms: Table<Room>;
  syncQueue: Table<SyncEvent>;
  photos: Table<Photo>;
  
  constructor() {
    super('InventoryDB');
    this.version(1).stores({
      items: '++id, name, boxId, *tags, lastModified',
      boxes: '++id, name, shelfId, roomId, lastModified',
      shelves: '++id, name, roomId, lastModified',
      rooms: '++id, name, lastModified',
      syncQueue: '++id, timestamp, type, status',
      photos: '++id, itemId, blob, lastModified'
    });
  }
}
```

**Sync Queue Pattern:**
```typescript
interface SyncEvent {
  id: string;
  timestamp: number;
  type: 'CREATE' | 'UPDATE' | 'DELETE' | 'MOVE';
  entity: 'item' | 'box' | 'shelf' | 'room';
  entityId: string;
  data: any;
  status: 'pending' | 'syncing' | 'synced' | 'conflict';
}

// Beispiel: Item verschieben
async function moveItem(itemId: string, newBoxId: string) {
  // 1. Lokal sofort ändern
  await db.items.update(itemId, { boxId: newBoxId });
  
  // 2. In Sync Queue eintragen
  await db.syncQueue.add({
    id: crypto.randomUUID(),
    timestamp: Date.now(),
    type: 'MOVE',
    entity: 'item',
    entityId: itemId,
    data: { newBoxId },
    status: 'pending'
  });
  
  // 3. Sync versuchen (wenn online)
  if (navigator.onLine) {
    await syncQueue.process();
  }
}
```

---

### 3.2 Backend (Quarkus)

#### Technologie-Stack
```xml
<!-- pom.xml dependencies -->
- quarkus-resteasy-reactive-jackson (REST API)
- quarkus-hibernate-orm-panache (ORM)
- quarkus-jdbc-postgresql (DB Driver)
- quarkus-oidc (Keycloak Integration)
- quarkus-websockets (Real-time Updates)
- quarkus-flyway (DB Migrations)
- quarkus-smallrye-openapi (API Documentation)
- quarkus-amazon-s3 (File Storage)
```

#### Package-Struktur
```
backend/
└── src/main/java/de/inventory/
    ├── api/
    │   ├── ItemResource.java
    │   ├── StorageResource.java
    │   ├── SyncResource.java
    │   ├── HistoryResource.java
    │   └── ExportResource.java
    ├── service/
    │   ├── ItemService.java
    │   ├── StorageService.java
    │   ├── SyncService.java
    │   ├── ConflictResolver.java
    │   ├── TaggingService.java
    │   └── ExportService.java
    ├── repository/
    │   ├── ItemRepository.java
    │   ├── BoxRepository.java
    │   ├── ShelfRepository.java
    │   ├── RoomRepository.java
    │   ├── SyncEventRepository.java
    │   └── AuditLogRepository.java
    ├── model/
    │   ├── entity/
    │   │   ├── Item.java
    │   │   ├── Box.java
    │   │   ├── Shelf.java
    │   │   ├── Room.java
    │   │   ├── SyncEvent.java
    │   │   ├── AuditLog.java
    │   │   └── Photo.java
    │   └── dto/
    │       ├── ItemDTO.java
    │       ├── MoveRequestDTO.java
    │       └── SyncBatchDTO.java
    ├── websocket/
    │   └── SyncWebSocket.java
    └── util/
        ├── S3Manager.java
        └── QRCodeGenerator.java
```

#### Datenmodell (JPA Entities)

```java
@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "box_id")
    private Box box;
    
    @ManyToOne
    @JoinColumn(name = "shelf_id")
    private Shelf shelf;
    
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
    
    private String position; // "hintere linke Ecke"
    
    @ElementCollection
    @CollectionTable(name = "item_tags")
    private Set<String> tags;
    
    private Integer quantity = 1;
    
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<Photo> photos;
    
    private String barcode;
    private String qrCode;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Version
    private Long version; // Optimistic Locking
    
    private String userId; // Owner
}

@Entity
@Table(name = "boxes")
public class Box {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "parent_box_id")
    private Box parentBox; // Verschachtelung
    
    @OneToMany(mappedBy = "parentBox")
    private List<Box> childBoxes;
    
    @ManyToOne
    @JoinColumn(name = "shelf_id")
    private Shelf shelf;
    
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
    
    private String position;
    
    @OneToMany(mappedBy = "box")
    private List<Item> items;
    
    private String qrCode;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Version
    private Long version;
    
    private String userId;
}

@Entity
@Table(name = "shelves")
public class Shelf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
    
    private String position;
    
    @OneToMany(mappedBy = "shelf")
    private List<Box> boxes;
    
    @OneToMany(mappedBy = "shelf")
    private List<Item> items;
    
    private String qrCode;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Version
    private Long version;
    
    private String userId;
}

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private LocationType type; // PERMANENT, TEMPORARY
    
    private String location; // "Keller", "Ferienfreizeit 2025"
    
    @OneToMany(mappedBy = "room")
    private List<Shelf> shelves;
    
    @OneToMany(mappedBy = "room")
    private List<Box> boxes;
    
    @OneToMany(mappedBy = "room")
    private List<Item> items;
    
    private String qrCode;
    
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Version
    private Long version;
    
    private String userId;
}

@Entity
@Table(name = "sync_events")
public class SyncEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String clientId; // Welches Gerät
    private String userId;
    
    @Enumerated(EnumType.STRING)
    private EventType type; // CREATE, UPDATE, DELETE, MOVE
    
    @Enumerated(EnumType.STRING)
    private EntityType entity; // ITEM, BOX, SHELF, ROOM
    
    private Long entityId;
    
    @Column(columnDefinition = "jsonb")
    private String data; // JSON payload
    
    private LocalDateTime timestamp;
    private Long vectorClock; // Für Konflikt-Erkennung
    
    @Enumerated(EnumType.STRING)
    private SyncStatus status; // PENDING, APPLIED, CONFLICT
}

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userId;
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private ActionType action; // CREATE, UPDATE, DELETE, MOVE, etc.
    
    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    
    private Long entityId;
    private String entityName;
    
    @Column(columnDefinition = "jsonb")
    private String oldValue;
    
    @Column(columnDefinition = "jsonb")
    private String newValue;
    
    private String description; // Human-readable
}

@Entity
@Table(name = "photos")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;
    
    private String s3Key; // S3 Bucket Key
    private String s3Url; // Presigned URL
    
    private Boolean isPrimary = false;
    
    private LocalDateTime uploadedAt;
    private String userId;
}
```

#### REST API Endpoints

```java
@Path("/api/v1/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {
    
    @Inject ItemService itemService;
    
    // Alle Items abrufen (mit Paging)
    @GET
    public Response getItems(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size,
        @QueryParam("search") String search,
        @QueryParam("tags") List<String> tags
    ) { ... }
    
    // Item nach ID
    @GET
    @Path("/{id}")
    public Response getItem(@PathParam("id") Long id) { ... }
    
    // Neues Item erstellen
    @POST
    public Response createItem(ItemDTO dto) { ... }
    
    // Item aktualisieren
    @PUT
    @Path("/{id}")
    public Response updateItem(@PathParam("id") Long id, ItemDTO dto) { ... }
    
    // Item löschen
    @DELETE
    @Path("/{id}")
    public Response deleteItem(@PathParam("id") Long id) { ... }
    
    // Item verschieben
    @POST
    @Path("/{id}/move")
    public Response moveItem(@PathParam("id") Long id, MoveRequestDTO dto) { ... }
    
    // Suche mit Fuzzy-Matching
    @GET
    @Path("/search")
    public Response searchItems(@QueryParam("q") String query) { ... }
    
    // QR-Code generieren
    @GET
    @Path("/{id}/qr-code")
    @Produces("image/png")
    public Response getQRCode(@PathParam("id") Long id) { ... }
    
    // Foto hochladen
    @POST
    @Path("/{id}/photos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPhoto(@PathParam("id") Long id, 
                                 @MultipartForm PhotoUploadForm form) { ... }
}

@Path("/api/v1/sync")
public class SyncResource {
    
    @Inject SyncService syncService;
    
    // Batch-Sync für Offline-Änderungen
    @POST
    @Path("/batch")
    public Response syncBatch(SyncBatchDTO batch) { ... }
    
    // Änderungen seit Timestamp abrufen
    @GET
    @Path("/changes")
    public Response getChanges(
        @QueryParam("since") Long timestamp,
        @QueryParam("clientId") String clientId
    ) { ... }
    
    // Konflikte abrufen
    @GET
    @Path("/conflicts")
    public Response getConflicts() { ... }
    
    // Konflikt auflösen
    @POST
    @Path("/conflicts/{id}/resolve")
    public Response resolveConflict(@PathParam("id") Long id, 
                                     ConflictResolutionDTO dto) { ... }
}

@Path("/api/v1/export")
public class ExportResource {
    
    @Inject ExportService exportService;
    
    // CSV Export
    @GET
    @Path("/csv")
    @Produces("text/csv")
    public Response exportCSV(@QueryParam("roomId") Long roomId) { ... }
    
    // PDF Export
    @GET
    @Path("/pdf")
    @Produces("application/pdf")
    public Response exportPDF(@QueryParam("roomId") Long roomId) { ... }
    
    // Etiketten generieren
    @POST
    @Path("/labels")
    @Produces("application/pdf")
    public Response generateLabels(List<Long> boxIds) { ... }
}
```

#### Synchronisations-Algorithmus

```java
@ApplicationScoped
public class SyncService {
    
    @Inject SyncEventRepository syncEventRepo;
    @Inject ConflictResolver conflictResolver;
    
    @Transactional
    public SyncResult processSyncBatch(SyncBatchDTO batch) {
        List<SyncEvent> conflicts = new ArrayList<>();
        List<SyncEvent> applied = new ArrayList<>();
        
        for (SyncEventDTO event : batch.getEvents()) {
            // 1. Prüfe ob Server neuere Version hat
            Long serverVersion = getServerVersion(event.getEntityType(), event.getEntityId());
            
            if (serverVersion > event.getClientVersion()) {
                // Konflikt!
                SyncEvent conflict = createConflictEvent(event);
                conflicts.add(conflict);
                continue;
            }
            
            // 2. Event anwenden
            try {
                applyEvent(event);
                applied.add(createAppliedEvent(event));
            } catch (Exception e) {
                conflicts.add(createErrorEvent(event, e));
            }
        }
        
        return new SyncResult(applied, conflicts);
    }
    
    private void applyEvent(SyncEventDTO event) {
        switch (event.getType()) {
            case CREATE:
                createEntity(event);
                break;
            case UPDATE:
                updateEntity(event);
                break;
            case DELETE:
                deleteEntity(event);
                break;
            case MOVE:
                moveEntity(event);
                break;
        }
        
        // Audit Log schreiben
        auditLogService.log(event);
    }
}
```

#### WebSocket für Real-time Updates

```java
@ServerEndpoint("/ws/sync/{userId}")
public class SyncWebSocket {
    
    private static Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        sessions.put(userId, session);
    }
    
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        sessions.remove(userId);
    }
    
    public static void broadcastUpdate(String userId, SyncEvent event) {
        Session session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(
                Json.encode(event)
            );
        }
    }
}

// In Service-Methoden nach Updates:
@Transactional
public Item moveItem(Long itemId, MoveRequest request) {
    Item item = itemRepository.findById(itemId);
    item.setBox(boxRepository.findById(request.getNewBoxId()));
    item.setLastModified(LocalDateTime.now());
    item = itemRepository.persist(item);
    
    // Audit Log
    auditLogService.logMove(item, request.getOldBoxId(), request.getNewBoxId());
    
    // WebSocket Broadcast
    SyncWebSocket.broadcastUpdate(item.getUserId(), createSyncEvent(item));
    
    return item;
}
```

---

### 3.3 Datenbank (PostgreSQL)

#### Schema Migration (Flyway)

```sql
-- V1__initial_schema.sql

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- PERMANENT, TEMPORARY
    location VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE shelves (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    room_id BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    position VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE boxes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_box_id BIGINT REFERENCES boxes(id) ON DELETE SET NULL,
    shelf_id BIGINT REFERENCES shelves(id) ON DELETE SET NULL,
    room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    position VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraint: Box muss entweder in parent_box, shelf oder room sein
    CONSTRAINT box_location_check CHECK (
        (parent_box_id IS NOT NULL AND shelf_id IS NULL AND room_id IS NULL) OR
        (parent_box_id IS NULL AND shelf_id IS NOT NULL AND room_id IS NULL) OR
        (parent_box_id IS NULL AND shelf_id IS NULL AND room_id IS NOT NULL)
    )
);

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    box_id BIGINT REFERENCES boxes(id) ON DELETE SET NULL,
    shelf_id BIGINT REFERENCES shelves(id) ON DELETE SET NULL,
    room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    position VARCHAR(255),
    quantity INTEGER NOT NULL DEFAULT 1,
    barcode VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraint: Item muss irgendwo sein
    CONSTRAINT item_location_check CHECK (
        box_id IS NOT NULL OR shelf_id IS NOT NULL OR room_id IS NOT NULL
    )
);

CREATE TABLE item_tags (
    item_id BIGINT REFERENCES items(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (item_id, tag)
);

CREATE TABLE photos (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT REFERENCES items(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    s3_url VARCHAR(1000),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id VARCHAR(255) NOT NULL
);

CREATE TABLE sync_events (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, MOVE
    entity VARCHAR(50) NOT NULL, -- ITEM, BOX, SHELF, ROOM
    entity_id BIGINT NOT NULL,
    data JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    vector_clock BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    entity_name VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    description TEXT
);

-- Indizes für Performance
CREATE INDEX idx_items_box_id ON items(box_id);
CREATE INDEX idx_items_shelf_id ON items(shelf_id);
CREATE INDEX idx_items_room_id ON items(room_id);
CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_last_modified ON items(last_modified);

CREATE INDEX idx_boxes_shelf_id ON boxes(shelf_id);
CREATE INDEX idx_boxes_room_id ON boxes(room_id);
CREATE INDEX idx_boxes_parent_box_id ON boxes(parent_box_id);
CREATE INDEX idx_boxes_user_id ON boxes(user_id);

CREATE INDEX idx_shelves_room_id ON shelves(room_id);
CREATE INDEX idx_shelves_user_id ON shelves(user_id);

CREATE INDEX idx_rooms_user_id ON rooms(user_id);
CREATE INDEX idx_rooms_type ON rooms(type);

CREATE INDEX idx_sync_events_user_id ON sync_events(user_id);
CREATE INDEX idx_sync_events_timestamp ON sync_events(timestamp);
CREATE INDEX idx_sync_events_status ON sync_events(status);

CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- Full-Text-Search für Items (für Fuzzy-Search)
CREATE INDEX idx_items_name_trgm ON items USING gin(name gin_trgm_ops);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

#### Performance-Optimierung

```sql
-- V2__performance_optimization.sql

-- Materialized View für schnelle Hierarchie-Abfragen
CREATE MATERIALIZED VIEW item_locations AS
SELECT 
    i.id as item_id,
    i.name as item_name,
    i.quantity,
    b.id as box_id,
    b.name as box_name,
    s.id as shelf_id,
    s.name as shelf_name,
    r.id as room_id,
    r.name as room_name,
    r.location as room_location,
    COALESCE(
        r.name || ' > ' || COALESCE(s.name || ' > ', '') || COALESCE(b.name, ''),
        'Unbekannter Ort'
    ) as full_path
FROM items i
LEFT JOIN boxes b ON i.box_id = b.id
LEFT JOIN shelves s ON COALESCE(b.shelf_id, i.shelf_id) = s.id
LEFT JOIN rooms r ON COALESCE(s.room_id, b.room_id, i.room_id) = r.id;

CREATE UNIQUE INDEX ON item_locations(item_id);

-- Refresh Trigger
CREATE OR REPLACE FUNCTION refresh_item_locations()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY item_locations;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER refresh_item_locations_trigger
AFTER INSERT OR UPDATE OR DELETE ON items
FOR EACH STATEMENT
EXECUTE FUNCTION refresh_item_locations();
```

---

### 3.4 Authentication (Keycloak)

#### Konfiguration

```yaml
# Keycloak Realm: inventory-system
realm: inventory-system
clients:
  - clientId: inventory-frontend
    publicClient: true
    redirectUris:
      - "https://inventory.example.com/*"
      - "http://localhost:3000/*"
    webOrigins:
      - "https://inventory.example.com"
      - "http://localhost:3000"
    
  - clientId: inventory-backend
    publicClient: false
    serviceAccountsEnabled: true
    authorizationServicesEnabled: true

roles:
  - name: user
    description: Standard user role
  - name: admin
    description: Administrator role
```

#### Backend Integration

```java
// application.properties
quarkus.oidc.auth-server-url=https://keycloak.example.com/realms/inventory-system
quarkus.oidc.client-id=inventory-backend
quarkus.oidc.credentials.secret=${KEYCLOAK_CLIENT_SECRET}
quarkus.oidc.tls.verification=required

// Secured Endpoint
@Path("/api/v1/items")
public class ItemResource {
    
    @Inject SecurityIdentity identity;
    
    @GET
    @RolesAllowed("user")
    public Response getItems() {
        String userId = identity.getPrincipal().getName();
        // Nur Items des Users zurückgeben
        return Response.ok(itemService.getItemsByUser(userId)).build();
    }
}
```

#### Frontend Integration

```typescript
// plugins/auth.client.ts
export default defineNuxtPlugin(async () => {
  const config = useRuntimeConfig()
  
  const keycloak = new Keycloak({
    url: config.public.keycloakUrl,
    realm: 'inventory-system',
    clientId: 'inventory-frontend'
  })
  
  await keycloak.init({
    onLoad: 'check-sso',
    silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
  })
  
  return {
    provide: {
      keycloak,
      auth: {
        login: () => keycloak.login(),
        logout: () => keycloak.logout(),
        getToken: () => keycloak.token,
        isAuthenticated: () => keycloak.authenticated
      }
    }
  }
})
```

---

### 3.5 File Storage (AWS S3)

#### S3 Bucket-Struktur

```
inventory-photos/
├── users/
│   └── {userId}/
│       └── items/
│           └── {itemId}/
│               ├── original/
│               │   └── {photoId}.jpg
│               └── thumbnails/
│                   ├── {photoId}_small.jpg
│                   ├── {photoId}_medium.jpg
│                   └── {photoId}_large.jpg
└── qr-codes/
    ├── items/
    │   └── {itemId}.png
    ├── boxes/
    │   └── {boxId}.png
    ├── shelves/
    │   └── {shelfId}.png
    └── rooms/
        └── {roomId}.png
```

#### Backend S3 Integration

```java
@ApplicationScoped
public class S3Manager {
    
    @Inject
    @ConfigProperty(name = "s3.bucket.name")
    String bucketName;
    
    @Inject
    S3Client s3Client;
    
    public String uploadPhoto(Long itemId, String userId, byte[] photoData) {
        String key = String.format("users/%s/items/%d/original/%s.jpg", 
                                   userId, itemId, UUID.randomUUID());
        
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("image/jpeg")
            .build();
        
        s3Client.putObject(request, RequestBody.fromBytes(photoData));
        
        // Thumbnail generieren (async)
        CompletableFuture.runAsync(() -> generateThumbnails(key, photoData));
        
        return key;
    }
    
    public String getPresignedUrl(String s3Key, Duration expiration) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(req -> req.bucket(bucketName).key(s3Key))
            .build();
        
        PresignedGetObjectRequest presignedRequest = 
            s3Presigner.presignGetObject(presignRequest);
        
        return presignedRequest.url().toString();
    }
    
    private void generateThumbnails(String originalKey, byte[] originalData) {
        // Small: 200x200
        byte[] small = ImageCompressor.resize(originalData, 200, 200);
        uploadThumbnail(originalKey, "small", small);
        
        // Medium: 600x600
        byte[] medium = ImageCompressor.resize(originalData, 600, 600);
        uploadThumbnail(originalKey, "medium", medium);
        
        // Large: 1200x1200
        byte[] large = ImageCompressor.resize(originalData, 1200, 1200);
        uploadThumbnail(originalKey, "large", large);
    }
}
```

#### Frontend Photo Upload

```typescript
// composables/usePhotoUpload.ts
export const usePhotoUpload = () => {
  const compress = async (file: File): Promise<Blob> => {
    const options = {
      maxSizeMB: 0.5,
      maxWidthOrHeight: 1920,
      useWebWorker: true
    }
    return await imageCompression(file, options)
  }
  
  const upload = async (itemId: number, file: File) => {
    const compressed = await compress(file)
    
    const formData = new FormData()
    formData.append('photo', compressed)
    
    const response = await $fetch(`/api/v1/items/${itemId}/photos`, {
      method: 'POST',
      body: formData
    })
    
    return response
  }
  
  return { upload, compress }
}
```

---

## 4. Deployment-Architektur (AWS)

### 4.1 AWS Services

```
┌─────────────────────────────────────────────────────────┐
│                    CloudFront (CDN)                      │
│              - Frontend Static Assets                    │
│              - Global Edge Locations                     │
└────────────────────┬────────────────────────────────────┘
                     │
         ┌───────────┴──────────┐
         ▼                      ▼
┌──────────────────┐   ┌──────────────────────┐
│   S3 Bucket      │   │  Application LB      │
│   (Frontend)     │   │  (Backend API)       │
└──────────────────┘   └──────────┬───────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
        ┌──────────────────┐          ┌──────────────────┐
        │  ECS Fargate     │          │  ECS Fargate     │
        │  (Quarkus)       │          │  (Keycloak)      │
        │  - Auto Scaling  │          └──────────────────┘
        │  - Multi-AZ      │
        └────────┬─────────┘
                 │
                 ▼
        ┌──────────────────┐
        │  RDS PostgreSQL  │
        │  - Multi-AZ      │
        │  - Auto Backup   │
        └──────────────────┘
                 │
                 ▼
        ┌──────────────────┐
        │  S3 Bucket       │
        │  (Photos)        │
        └──────────────────┘
```

### 4.2 Terraform Infrastructure as Code

```hcl
# main.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "inventory-terraform-state"
    key    = "prod/terraform.tfstate"
    region = "eu-central-1"
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  
  name = "inventory-vpc"
  cidr = "10.0.0.0/16"
  
  azs             = ["eu-central-1a", "eu-central-1b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]
  
  enable_nat_gateway = true
  single_nat_gateway = true # Cost optimization
  
  enable_dns_hostnames = true
  enable_dns_support   = true
}

# RDS PostgreSQL
resource "aws_db_instance" "postgres" {
  identifier = "inventory-db"
  
  engine         = "postgres"
  engine_version = "16.1"
  instance_class = "db.t3.micro" # Für Start, später upgraden
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_encrypted     = true
  
  db_name  = "inventory"
  username = var.db_username
  password = var.db_password
  
  multi_az               = true
  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"
  
  skip_final_snapshot = false
  final_snapshot_identifier = "inventory-db-final-snapshot"
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "inventory-cluster"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# ECS Task Definition - Backend
resource "aws_ecs_task_definition" "backend" {
  family                   = "inventory-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn           = aws_iam_role.ecs_task.arn
  
  container_definitions = jsonencode([{
    name  = "backend"
    image = "${aws_ecr_repository.backend.repository_url}:latest"
    
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    
    environment = [
      {
        name  = "QUARKUS_DATASOURCE_JDBC_URL"
        value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/inventory"
      },
      {
        name  = "QUARKUS_OIDC_AUTH_SERVER_URL"
        value = "https://${aws_lb.keycloak.dns_name}/realms/inventory-system"
      }
    ]
    
    secrets = [
      {
        name      = "QUARKUS_DATASOURCE_USERNAME"
        valueFrom = aws_secretsmanager_secret.db_credentials.arn
      },
      {
        name      = "QUARKUS_DATASOURCE_PASSWORD"
        valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:password::"
      }
    ]
    
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/inventory-backend"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }
    
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:8080/q/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
  }])
}

# ECS Service - Backend
resource "aws_ecs_service" "backend" {
  name            = "inventory-backend-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = 2
  launch_type     = "FARGATE"
  
  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }
  
  depends_on = [aws_lb_listener.backend]
}

# Application Load Balancer
resource "aws_lb" "backend" {
  name               = "inventory-backend-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.vpc.public_subnets
}

resource "aws_lb_target_group" "backend" {
  name        = "inventory-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"
  
  health_check {
    path                = "/q/health"
    healthy_threshold   = 2
    unhealthy_threshold = 10
    timeout             = 5
    interval            = 30
  }
}

resource "aws_lb_listener" "backend" {
  load_balancer_arn = aws_lb.backend.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.backend.arn
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# S3 Bucket für Frontend
resource "aws_s3_bucket" "frontend" {
  bucket = "inventory-frontend-${var.environment}"
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  
  index_document {
    suffix = "index.html"
  }
  
  error_document {
    key = "index.html" # SPA routing
  }
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  default_root_object = "index.html"
  
  origin {
    domain_name = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id   = "S3-${aws_s3_bucket.frontend.id}"
    
    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.frontend.cloudfront_access_identity_path
    }
  }
  
  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.frontend.id}"
    viewer_protocol_policy = "redirect-to-https"
    
    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
    
    min_ttl     = 0
    default_ttl = 3600
    max_ttl     = 86400
  }
  
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  
  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate.frontend.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

# S3 Bucket für Fotos
resource "aws_s3_bucket" "photos" {
  bucket = "inventory-photos-${var.environment}"
}

resource "aws_s3_bucket_lifecycle_configuration" "photos" {
  bucket = aws_s3_bucket.photos.id
  
  rule {
    id     = "delete-old-thumbnails"
    status = "Enabled"
    
    filter {
      prefix = "users/"
    }
    
    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }
    
    expiration {
      days = 365
    }
  }
}
```

### 4.3 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS

on:
  push:
    branches: [main]

env:
  AWS_REGION: eu-central-1
  ECR_REPOSITORY: inventory-backend
  ECS_SERVICE: inventory-backend-service
  ECS_CLUSTER: inventory-cluster
  CONTAINER_NAME: backend

jobs:
  build-and-deploy-backend:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v1
      
      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          cd backend
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
                       -t $ECR_REGISTRY/$ECR_REPOSITORY:latest .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
      
      - name: Update ECS service
        run: |
          aws ecs update-service \
            --cluster $ECS_CLUSTER \
            --service $ECS_SERVICE \
            --force-new-deployment
  
  build-and-deploy-frontend:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'
      
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
      
      - name: Build
        run: |
          cd frontend
          npm run generate
        env:
          NUXT_PUBLIC_API_URL: https://api.inventory.example.com
          NUXT_PUBLIC_KEYCLOAK_URL: https://auth.inventory.example.com
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}
      
      - name: Deploy to S3
        run: |
          aws s3 sync frontend/.output/public s3://inventory-frontend-prod \
            --delete \
            --cache-control "public, max-age=31536000, immutable"
      
      - name: Invalidate CloudFront
        run: |
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"
```

---

## 5. Kosten-Abschätzung (AWS)

### Monatliche Kosten (geschätzt für Hobby-Projekt)

| Service | Spezifikation | Kosten/Monat |
|---------|--------------|--------------|
| **ECS Fargate** | 2x 0.5 vCPU, 1GB RAM (Backend) | ~$30 |
| **RDS PostgreSQL** | db.t3.micro, Multi-AZ, 20GB | ~$35 |
| **S3 (Frontend)** | 1GB Storage + Transfer | ~$1 |
| **S3 (Photos)** | 10GB Storage + Transfer | ~$3 |
| **CloudFront** | 10GB Data Transfer | ~$1 |
| **Application LB** | Standard ALB | ~$20 |
| **Route53** | Hosted Zone + Queries | ~$1 |
| **Certificate Manager** | SSL Certs | $0 (kostenlos) |
| **ECS Fargate (Keycloak)** | 1x 0.5 vCPU, 1GB RAM | ~$15 |
| **Secrets Manager** | 2 Secrets | ~$1 |
| **CloudWatch Logs** | 5GB/Monat | ~$3 |
| **GESAMT** | | **~$110/Monat** |

### Kosten-Optimierungs-Optionen

1. **Hetzner Cloud statt AWS** (~€15/Monat)
   - 1x CPX21 (3 vCPU, 4GB RAM) für Backend + Keycloak
   - PostgreSQL + S3-Alternative (Object Storage)
   - Traefik als Reverse Proxy
   
2. **AWS Lightsail** (~$40/Monat)
   - Vereinfachtes AWS für kleine Apps
   - Fixed Pricing
   
3. **Hybrid**
   - Backend auf Hetzner
   - Frontend auf AWS S3 + CloudFront

---

## 6. Alternative: Hetzner Deployment (Cost-Optimized)

```yaml
# docker-compose.yml (für Hetzner Server)
version: '3.8'

services:
  traefik:
    image: traefik:v2.10
    container_name: traefik
    command:
      - "--api.insecure=false"
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.email=admin@example.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./letsencrypt:/letsencrypt
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: postgres
    environment:
      POSTGRES_DB: inventory
      POSTGRES_USER: inventory_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U inventory_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: keycloak
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/inventory
      KC_DB_USERNAME: inventory_user
      KC_DB_PASSWORD: ${DB_PASSWORD}
      KC_HOSTNAME: auth.inventory.example.com
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    command: start --proxy edge
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.keycloak.rule=Host(`auth.inventory.example.com`)"
      - "traefik.http.routers.keycloak.entrypoints=websecure"
      - "traefik.http.routers.keycloak.tls.certresolver=letsencrypt"
      - "traefik.http.services.keycloak.loadbalancer.server.port=8080"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  backend:
    image: ghcr.io/yourusername/inventory-backend:latest
    container_name: backend
    environment:
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/inventory
      QUARKUS_DATASOURCE_USERNAME: inventory_user
      QUARKUS_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      QUARKUS_OIDC_AUTH_SERVER_URL: https://auth.inventory.example.com/realms/inventory-system
      S3_ENDPOINT: https://s3.eu-central-1.amazonaws.com
      S3_BUCKET_NAME: inventory-photos
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.backend.rule=Host(`api.inventory.example.com`)"
      - "traefik.http.routers.backend.entrypoints=websecure"
      - "traefik.http.routers.backend.tls.certresolver=letsencrypt"
      - "traefik.http.services.backend.loadbalancer.server.port=8080"
    depends_on:
      postgres:
        condition: service_healthy
      keycloak:
        condition: service_started
    restart: unless-stopped

  # Optional: MinIO als S3-Alternative
  minio:
    image: minio/minio:latest
    container_name: minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio_data:/data
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.minio.rule=Host(`s3.inventory.example.com`)"
      - "traefik.http.routers.minio.entrypoints=websecure"
      - "traefik.http.routers.minio.tls.certresolver=letsencrypt"
      - "traefik.http.services.minio.loadbalancer.server.port=9000"
      - "traefik.http.routers.minio-console.rule=Host(`minio.inventory.example.com`)"
      - "traefik.http.routers.minio-console.entrypoints=websecure"
      - "traefik.http.routers.minio-console.tls.certresolver=letsencrypt"
      - "traefik.http.services.minio-console.loadbalancer.server.port=9001"
    restart: unless-stopped

volumes:
  postgres_data:
  minio_data:
```

---

## 7. Nächste Schritte

### Phase 1: MVP (Minimum Viable Product)
**Zeitrahmen: 4-6 Wochen**

1. **Woche 1-2: Backend Basic**
   - [ ] Quarkus Projekt Setup
   - [ ] Datenmodell (Items, Boxes, Shelves, Rooms)
   - [ ] Basic CRUD REST API
   - [ ] Keycloak Integration
   - [ ] PostgreSQL Setup mit Flyway

2. **Woche 2-3: Frontend Basic**
   - [ ] Nuxt.js PWA Setup
   - [ ] Login/Logout mit Keycloak
   - [ ] Item Liste & Detail-Ansicht
   - [ ] Einfache Suche
   - [ ] Storage Hierarchie Navigation

3. **Woche 3-4: Offline-Funktionalität**
   - [ ] IndexedDB Integration
   - [ ] Service Worker Setup
   - [ ] Sync Queue Implementation
   - [ ] Conflict Resolution (einfach: last-write-wins)

4. **Woche 4-5: Move & QR Codes**
   - [ ] Move Items/Boxes API
   - [ ] QR-Code Generierung
   - [ ] QR-Code Scanner
   - [ ] Schnelles Umräumen per QR

5. **Woche 5-6: Deployment & Testing**
   - [ ] Docker Images
   - [ ] AWS/Hetzner Setup
   - [ ] CI/CD Pipeline
   - [ ] User Acceptance Testing

### Phase 2: Erweiterungen
**Zeitrahmen: 4-8 Wochen**

1. **Fotos**
   - S3/MinIO Integration
   - Photo Upload & Compression
   - Thumbnail-Generierung

2. **History/Audit**
   - Audit Log Implementation
   - History Timeline UI
   - Undo-Funktionalität

3. **Export**
   - CSV/PDF Export
   - Etiketten-Generierung
   - Packzettel

4. **Mengen & Multi-Location**
   - Mehrere Exemplare
   - Standort-Gruppen
   - Inventory Count

### Phase 3: Advanced Features
**Zeitrahmen: Nach Bedarf**

1. **Tagging & AI**
   - Automatisches Tagging
   - Fuzzy-Search
   - Bilderkennung

2. **Verleih**
   - Verleih-Management
   - Kalender-Integration
   - Erinnerungen

---

## 8. Offene Fragen & Entscheidungen

1. **Cloud Provider**: AWS oder Hetzner?
   - AWS: Teurer, aber mehr Features, besser skalierbar
   - Hetzner: Günstiger, ausreichend für Start
   - **Empfehlung**: Start mit Hetzner, später Migration zu AWS wenn nötig

2. **S3 vs. MinIO**:
   - AWS S3: Managed, teurer
   - MinIO: Self-hosted, günstiger, mehr Kontrolle
   - **Empfehlung**: MinIO für Start (auf Hetzner)

3. **Sync-Strategie**:
   - Last-Write-Wins: Einfach, aber Datenverlust möglich
   - Event Sourcing: Komplex, aber robust
   - **Empfehlung**: Start mit Last-Write-Wins + Version-Check

4. **Multi-User**:
   - Jetzt schon oder später?
   - **Empfehlung**: Datenmodell vorbereiten (userId), UI später

Möchtest du bei einem dieser Punkte tiefer einsteigen oder soll ich mit der Implementierung eines spezifischen Teils beginnen?
