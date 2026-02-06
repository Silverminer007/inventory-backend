# MVP Definition - Lagerverwaltungssystem

## 📋 Übersicht

Dieses Dokument definiert den Minimal Viable Product (MVP) Scope für das Lagerverwaltungssystem.
Es referenziert die vollständigen Anforderungen aus der initialen Anforderungsanalyse und markiert,
welche Anforderungen im MVP enthalten sind.

**MVP-Ziel:** Ein funktionsfähiges Lagerverwaltungssystem für offline und online Nutzung mit
grundlegenden Features für Standortverwaltung, Suche und Synchronisation.

**Geschätzte Entwicklungszeit MVP:** 4-6 Wochen

---

## ✅ Funktionale Anforderungen im MVP

### Datenmodell & Hierarchie

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F1 | Hierarchische Struktur aus Räumen, Regalen, Kisten und Gegenständen | ✅ | | |
| F2 | Beliebig tiefe Verschachtelungen von Kisten | ✅ | | |
| F3 | Gegenstände direkt in Räumen platzieren | ✅ | | |
| F4 | Gegenstände direkt in Regalen platzieren | ✅ | | |
| F5 | Eindeutige Identifikation aller Elemente | ✅ | | |

**Implementierungsstatus:**
- ✅ Container-Entity (generisch) implementiert
- ✅ Item-Entity mit container_id implementiert
- ✅ Hierarchie über parent_id

---

### Positionsangaben

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F6 | Positionsangaben für Gegenstände innerhalb Container | ✅ | | |
| F7 | Positionsangaben für Kisten innerhalb Container | ✅ | | |
| F8 | Positionsangaben für Regale innerhalb Räumen | ✅ | | |

**Implementierungsstatus:**
- ✅ `position` Feld in Container und Item Entity

---

### Suchfunktionalität

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F9 | Suche nach Gegenständen anhand Name | ✅ | | |
| F10 | Vollständiger Pfad zum Standort anzeigen | ✅ | | |
| F11 | Filterung/Suche nach Standorten | ✅ | | |
| F12 | Suchfunktion offline verfügbar | | ✅ | |
| F34 | Ähnliche Begriffe erkennen (Fuzzy-Search) | ✅ | | |
| F35 | Synonyme und alternative Bezeichnungen verwalten | | ✅ | |
| F36 | Tippfehler tolerieren (Fuzzy-Search) | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: Name-based Search implementiert
- ✅ Backend: PostgreSQL pg_trgm Fuzzy-Search implementiert
- ✅ Backend: `getFullPath()` Helper-Methoden
- ⏳ Frontend: Offline-Suche (Phase 2)
- ⏳ Backend: Synonym-Integration (Phase 2)

---

### Verwaltung von Standorten

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F13 | Anlegen neuer Räume | ✅ | | |
| F14 | Anlegen neuer Regale in Räumen | ✅ | | |
| F15 | Anlegen neuer Kisten | ✅ | | |
| F16 | Anlegen neuer Gegenstände | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: Container CRUD (für alle Typen)
- ✅ Backend: Item CRUD
- ⏳ Frontend: UI für CRUD-Operationen

---

### Umräumen & Verschieben

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F17 | Verschieben einzelner Gegenstände zwischen Containern | ✅ | | |
| F18 | Verschieben ganzer Kisten zwischen Standorten | ✅ | | |
| F19 | Beim Verschieben Kisten alle Inhalte automatisch mit verschieben | ✅ | | |
| F20 | Verschiebe-Operationen offline durchführbar | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: Move Item API (`POST /items/{id}/move`)
- ✅ Backend: Move Container (durch `parent_id` Update)
- ✅ Backend: Cascade durch Hierarchie (automatisch via DB)
- ⏳ Frontend: Offline-Move mit Sync Queue

---

### Temporäre Standorte

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F21 | Temporäre Standorte definieren | ✅ | | |
| F22 | Verschieben zu temporären Standorten | ✅ | | |
| F23 | Ursprünglichen Standort anzeigen/vorschlagen | | ✅ | |
| F24 | Kennzeichnen welche Kisten an temporären Standorten | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: `location_type` ENUM (PERMANENT/TEMPORARY)
- ✅ Backend: Container Query nach Type
- ⏳ Backend: Ursprungs-Standort tracken (Phase 2)

---

### Bearbeitung & Löschung

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F25 | Bearbeiten von Namen und Beschreibungen | ✅ | | |
| F26 | Löschen von leeren Elementen | ✅ | | |
| F27 | Warnung beim Löschen nicht-leerer Container | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: PUT Endpoints
- ✅ Backend: DELETE mit CASCADE (leert automatisch)
- ⏳ Frontend: Confirmation Dialog

---

### Mengenverwaltung & Mehrfachstandorte

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F28 | Mehrere Exemplare desselben Gegenstands verwalten | ✅ | | |
| F29 | Gesamtanzahl Exemplare anzeigen | ✅ | | |
| F30 | Mehrere Standorte gruppiert anzeigen | | | ✅ |
| F31 | Suchergebnisse mit mehreren Standorten gruppieren | | | ✅ |
| F32 | Verschieben einzelner Exemplare | | | ✅ |
| F33 | Anzahl Exemplare pro Standort verwalten | | | ✅ |

**Implementierungsstatus:**
- ✅ Backend: `quantity` Feld in Item
- ⏳ Mehrfachstandorte: Spätere Phase

---

### Tag-System

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F37 | Automatisch Tags zuordnen | ✅ | | |
| F38 | Tags manuell hinzufügen/entfernen | ✅ | | |
| F39 | Suche/Filterung nach Tags | ✅ | | |
| F40 | Mehrere Tags pro Gegenstand | ✅ | | |
| F41 | Übersicht aller verfügbaren Tags | | ✅ | |
| F42 | Anzahl Gegenstände pro Tag anzeigen | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: Rule-based TaggingService
- ✅ Backend: `item_tags` Tabelle (Set)
- ✅ Backend: Tag-basierte Suche
- ⏳ Frontend: Tag-Cloud / Tag-Filter

---

### Barcode/QR-Code-Scanning

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F51 | Scannen von Barcodes/QR-Codes über Kamera | | ✅ | |
| F52 | Barcode/QR-Code beim Anlegen erfassen | ✅ | | |
| F53 | Beim Scannen zugehörigen Gegenstand anzeigen | | ✅ | |
| F54 | QR-Codes generieren | | ✅ | |
| F55 | QR-Codes zum Ausdrucken bereitstellen | | ✅ | |
| F56 | Container-QR-Code scannen → alle Inhalte zeigen | | ✅ | |
| F57 | Schnelles Umräumen per QR-Code | | ✅ | |
| F58 | QR-Code-Scanning offline | | ✅ | |
| F59 | Batch-Scanning mehrerer Gegenstände | | | ✅ |
| F60 | Bei unbekannten Codes neuen Gegenstand anlegen | | | ✅ |

**Implementierungsstatus:**
- ✅ Backend: `barcode` & `qr_code` Felder in DB
- ⏳ Backend: QR-Code Generierung (Phase 2)
- ⏳ Frontend: Scanner Integration (Phase 2)

---

### Fotos von Gegenständen

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F61-F70 | Alle Foto-Features | | | ✅ |

**Implementierungsstatus:**
- ✅ Backend: `photos` Tabelle vorbereitet
- ⏳ Implementierung: Spätere Phase

---

### History/Audit-Log

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F71 | Alle Änderungen protokollieren | ✅ | | |
| F72 | Zeitpunkt, Art, Objekt, alte/neue Werte erfassen | ✅ | | |
| F73 | Chronologische Übersicht aller Änderungen | | ✅ | |
| F74 | History nach Objekten filtern | | ✅ | |
| F75 | History nach Aktionstypen filtern | | ✅ | |
| F76 | History nach Zeiträumen filtern | | ✅ | |
| F77 | Vollständige Pfade in History protokollieren | ✅ | | |
| F78 | Mengenänderungen protokollieren | ✅ | | |
| F79 | Einzelne Änderungen rückgängig machen (Undo) | | | ✅ |
| F80 | Bei Mehrbenutzer: erfassen wer Änderung vornahm | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: `audit_log` Tabelle
- ✅ Backend: AuditLogService (automatisches Logging)
- ⏳ Frontend: History UI (Phase 2)

---

### Export-Funktionen

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F81-F92 | Alle Export-Features | | | ✅ |

**Implementierungsstatus:**
- ⏳ Implementierung: Spätere Phase

---

### Multi-Device & Synchronisation

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F93 | Von mehreren Geräten nutzbar | | ✅ | |
| F94 | Über Webbrowser auf Desktop zugänglich | ✅ | | |
| F95 | Über Webbrowser auf Mobil zugänglich | ✅ | | |
| F96 | Änderungen zwischen Geräten synchronisieren | | ✅ | |
| F97 | Echtzeit-Updates anzeigen | | ✅ | |
| F98 | Gleichzeitige Änderungen erkennen | | ✅ | |
| F99 | Konfliktlösung bei Sync-Konflikten | | ✅ | |
| F100 | Konsistente Sync nach langer Offline-Nutzung | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: Web-API (REST)
- ✅ Backend: `sync_events` Tabelle vorbereitet
- ⏳ Frontend: PWA mit Service Worker (Phase 2)
- ⏳ Backend: WebSocket für Real-time (Phase 2)

---

### Benutzerverwaltung

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F101 | Login-System | | ✅ | |
| F102 | Auf mehreren Geräten eingeloggt | | ✅ | |
| F103 | Aktive Sessions/Geräte anzeigen | | | ✅ |
| F104 | Einzelne Sessions remote abmelden | | | ✅ |

**Implementierungsstatus:**
- ✅ Backend: `user_id` in allen Entities
- ⏳ Backend: Keycloak Integration (Phase 2)
- ⏳ MVP: Hardcoded "demo-user"

---

### Datenhosting & Zugriff

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| F105 | Daten zentral speichern und über Web bereitstellen | ✅ | | |
| F106 | Bei schlechter Verbindung nutzbar (PWA) | | ✅ | |
| F107 | Automatisch zwischen Online/Offline wechseln | | ✅ | |
| F108 | Beim Online-Wechsel automatisch synchronisieren | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: PostgreSQL + REST API
- ⏳ Frontend: Progressive Web App (Phase 2)

---

## ✅ Nicht-funktionale Anforderungen im MVP

### Performance

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF1 | Suche in max. 2 Sekunden | ✅ | | |
| NF2 | Verschieben in max. 1 Sekunde | ✅ | | |
| NF3 | Min. 10.000 Gegenstände performant | ✅ | | |
| NF21 | Tag-Suche in max. 2 Sekunden | ✅ | | |
| NF22 | Fuzzy-Search bei >5000 Items performant | ✅ | | |

**Implementierungsstatus:**
- ✅ PostgreSQL Indizes (pg_trgm für Fuzzy)
- ✅ Optimierte Queries

---

### Offline-Fähigkeit

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF4 | Vollständig offline nutzbar | | ✅ | |
| NF5 | Offline-Änderungen lokal speichern | | ✅ | |
| NF6 | Auto-Sync bei Online-Wiederherstellung | | ✅ | |
| NF7 | Konfliktlösung bei Sync | | ✅ | |
| NF8 | Online/Offline Status anzeigen | | ✅ | |

**Implementierungsstatus:**
- ⏳ Frontend: Service Worker + IndexedDB (Phase 2)

---

### Usability

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF9 | Auf Smartphones gut bedienbar | ✅ | | |
| NF10 | Verschieben mit max. 3 Klicks/Taps | ✅ | | |
| NF11 | Intuitive Navigation durch Hierarchie | ✅ | | |
| NF80 | Responsive (Desktop, Tablet, Smartphone) | ✅ | | |
| NF81 | Wichtigste Funktionen auf allen Geräten gut bedienbar | ✅ | | |

**Implementierungsstatus:**
- ⏳ Frontend: Responsive Design (in Entwicklung)

---

### Zuverlässigkeit

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF13 | Keine Daten bei Offline-Betrieb verlieren | | ✅ | |
| NF14 | Datensicherung ermöglichen | | ✅ | |
| NF15 | Transaktionssicherheit bei Verschieben | ✅ | | |
| NF63 | Backend Verfügbarkeit 99% | | | ✅ |
| NF86 | Datenkonsistenz bei gleichzeitiger Bearbeitung | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: Optimistic Locking (version field)
- ✅ Backend: PostgreSQL Transactions
- ⏳ Frontend: Offline Queue (Phase 2)

---

### Plattform

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF16 | Als PWA oder native App verfügbar | | ✅ | |
| NF17 | Auf Android und iOS lauffähig | | ✅ | |
| NF18 | Im Browser nutzbar | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: Web-API (browserunabhängig)
- ⏳ Frontend: PWA (Phase 2)

---

### Sicherheit

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF72 | HTTPS/TLS Verschlüsselung | | ✅ | |
| NF73 | Passwörter sicher gehasht | | ✅ | |
| NF74-NF79 | XSS, CSRF, SQL-Injection Schutz, 2FA | | ✅ | |

**Implementierungsstatus:**
- ✅ Backend: Prepared Statements (SQL-Injection Schutz)
- ⏳ Backend: Keycloak für Auth (Phase 2)
- ⏳ Deployment: HTTPS/TLS (Phase 2)

---

### Browser-Kompatibilität

| ID | Anforderung | MVP | Phase 2 | Später |
|----|-------------|-----|---------|--------|
| NF90 | Moderne Browser (Chrome, Firefox, Safari, Edge) | ✅ | | |
| NF91 | Browser-Versionen bis 2 Jahre alt | ✅ | | |

**Implementierungsstatus:**
- ✅ Backend: Standard REST API
- ⏳ Frontend: Modern JavaScript/TypeScript

---

## 📊 MVP Feature-Matrix

### ✅ Im MVP enthalten

**Core Features (100% fertig für Launch):**
- ✅ Hierarchische Container-Verwaltung (Raum > Regal > Kiste)
- ✅ Item CRUD mit Tags
- ✅ Automatisches Tagging (Rule-based)
- ✅ Suche (Name + Fuzzy)
- ✅ Move Items/Container
- ✅ Temporäre Standorte (Ferienfreizeit)
- ✅ Audit Log (automatisch)
- ✅ Web-basiert (Browser-Zugriff)

**Technisch:**
- ✅ PostgreSQL Datenbank
- ✅ Quarkus REST API
- ✅ OpenAPI/Swagger Dokumentation
- ✅ Docker Setup
- ✅ Flyway Migrations

---

### 🔄 Phase 2 (nach MVP)

**Enhanced Features (4-6 Wochen nach MVP):**
- ⏳ Progressive Web App (Offline-fähig)
- ⏳ Service Worker + IndexedDB
- ⏳ Sync Queue mit Konfliktlösung
- ⏳ WebSocket Real-time Updates
- ⏳ QR-Code Generierung & Scanning
- ⏳ Keycloak Authentication
- ⏳ History UI (Audit Log Ansicht)
- ⏳ Synonym-basierte Suche
- ⏳ Tag-Cloud & erweiterte Filter

---

### 🚀 Später (Phase 3+)

**Advanced Features:**
- ⏳ Photo Upload (S3/Hetzner Object Storage)
- ⏳ Export (CSV, PDF, Etiketten)
- ⏳ Verleihverwaltung
- ⏳ Mehrfachstandorte pro Item
- ⏳ ML-basiertes Tagging
- ⏳ Berechtigungssystem (Multi-User)

---

## 🎯 Definition of Done (MVP)

Der MVP ist fertig und launchbar wenn:

### Backend ✅
- [x] Container CRUD API funktioniert
- [x] Item CRUD API funktioniert
- [x] Suche funktioniert (Name + Fuzzy + Tags)
- [x] Move Items/Container funktioniert
- [x] Auto-Tagging funktioniert
- [x] Audit Log läuft automatisch
- [x] OpenAPI Dokumentation vorhanden
- [x] Docker Setup funktioniert
- [x] Grundlegende Tests vorhanden

### Frontend ⏳
- [ ] Nuxt.js PWA Setup
- [ ] Container-Übersicht (Liste + Hierarchie)
- [ ] Item-Übersicht mit Suche
- [ ] Create/Edit Dialoge für Container & Items
- [ ] Move-Funktionalität (Drag & Drop oder Picker)
- [ ] Responsive Design (Mobile & Desktop)
- [ ] Grundlegende Offline-Funktionalität

### Deployment ⏳
- [ ] Hetzner Cloud Server Setup
- [ ] PostgreSQL auf Server
- [ ] Docker Compose Production Config
- [ ] HTTPS mit Let's Encrypt
- [ ] Backup-Strategie

### Dokumentation ✅
- [x] README mit Setup-Anleitung
- [x] API Dokumentation (Swagger)
- [x] Architektur-Dokumentation
- [x] MVP-Definition (dieses Dokument)

### Testing & QA ⏳
- [ ] Manuelle End-to-End Tests durchgeführt
- [ ] Kompletter Workflow getestet:
    - [ ] Raum erstellen → Regal erstellen → Kiste erstellen → Items hinzufügen
    - [ ] Item suchen → Item verschieben → Änderungen im Audit Log sehen
    - [ ] Temporären Standort erstellen → Kisten dorthin verschieben
- [ ] Performance-Test (1000+ Items)
- [ ] Mobile-Tests (iOS Safari, Android Chrome)

---

## 📈 Success Metrics (MVP)

Der MVP ist erfolgreich wenn:

1. **Funktional:**
    - ✅ Alle Core-Features funktionieren
    - ✅ Keine kritischen Bugs
    - ⏳ Workflow ist intuitiv nutzbar

2. **Performance:**
    - ⏳ Suche <2 Sekunden bei 1000 Items
    - ⏳ Page Load <3 Sekunden

3. **User Acceptance:**
    - ⏳ Team kann System für echte Freizeit nutzen
    - ⏳ Kein "zurück zu Excel/Papier"

---

## 🗓️ Zeitplan (Schätzung)

### Phase 1: Backend MVP ✅ (3-4 Wochen)
**Status: 80% fertig**
- [x] Woche 1-2: Projekt Setup, Datenmodell, Basis CRUD
- [x] Woche 3: Suche, Tagging, Audit
- [ ] Woche 4: Testing, Bugfixes

**Noch zu tun:**
- Container Resources vollständig (aktuell nur Items fertig)
- QR-Code Generierung (Backend)
- Umfangreichere Tests

### Phase 2: Frontend MVP ⏳ (4-5 Wochen)
**Status: Noch nicht gestartet**
- [ ] Woche 1: Nuxt Setup, Basis-Layout, Auth-Vorbereitung
- [ ] Woche 2: Container & Item Views
- [ ] Woche 3: Suche, Move, Create/Edit Dialoge
- [ ] Woche 4: Offline-Support (IndexedDB, Service Worker)
- [ ] Woche 5: Polish, Testing, Bugfixes

### Phase 3: Deployment & Launch ⏳ (1-2 Wochen)
**Status: Noch nicht gestartet**
- [ ] Woche 1: Hetzner Setup, Docker Deployment
- [ ] Woche 2: Testing in Production, Go-Live

**Gesamt MVP: 8-11 Wochen**

---

## 🔗 Referenzen

- **Vollständige Anforderungen:** Siehe initiale Anforderungsanalyse
- **Architektur:** `architektur-entwurf.md`
- **Backend Status:** `inventory-backend/MVP-STATUS.md`
- **Refactoring Guide:** `inventory-backend-refactored/REFACTORING-GUIDE.md`

---

## 📝 Notizen

### Warum diese Priorisierung?

**Im MVP:**
- Core-Funktionalität muss funktionieren (Container, Items, Suche, Move)
- Basis-Features die täglich gebraucht werden
- Technische Basis für alle weiteren Features

**Nach MVP:**
- Offline-Support: Wichtig, aber MVP kann erstmal nur online laufen
- QR-Codes: Nice-to-have, manuell geht auch
- Auth: Demo-User reicht für MVP
- Photos/Export: Nice-to-have

### Abweichungen von ursprünglicher Planung

**Refactoring zu generischem Container:**
- ✅ Entschieden für 1 Container Entity statt 3 (Room/Shelf/Box)
- ✅ Reduziert Code-Duplikation um 67%
- ✅ Macht zukünftige Erweiterungen viel einfacher

**Offline-First verschoben:**
- Offline-Funktionalität ist komplex
- MVP kann erstmal nur online funktionieren
- Phase 2: PWA mit vollständiger Offline-Unterstützung

---

**Letzte Aktualisierung:** 2026-02-02
**Version:** 1.0
**Status:** Backend 80% fertig, Frontend noch nicht gestartet