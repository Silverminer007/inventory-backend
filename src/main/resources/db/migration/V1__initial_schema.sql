-- Initial Schema for Inventory Management System

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- For fuzzy search

-- ============================================================
-- Core Tables
-- ============================================================

CREATE TABLE rooms (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       location_type VARCHAR(50) NOT NULL DEFAULT 'PERMANENT',  -- PERMANENT, TEMPORARY
                       location VARCHAR(255),  -- 'Keller', 'Ferienfreizeit 2025'
                       position VARCHAR(255),  -- Optional position description
                       qr_code VARCHAR(255) UNIQUE,
                       last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
                       version BIGINT NOT NULL DEFAULT 0,  -- Optimistic locking
                       user_id VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                       CONSTRAINT check_location_type CHECK (location_type IN ('PERMANENT', 'TEMPORARY'))
);

CREATE TABLE shelves (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
                         position VARCHAR(255),  -- 'links vorne', 'oberste Ebene'
                         qr_code VARCHAR(255) UNIQUE,
                         last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
                         version BIGINT NOT NULL DEFAULT 0,
                         user_id VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE boxes (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,

    -- Ein Box kann in verschiedenen Containern sein
                       parent_box_id BIGINT REFERENCES boxes(id) ON DELETE SET NULL,
                       shelf_id BIGINT REFERENCES shelves(id) ON DELETE SET NULL,
                       room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,

                       position VARCHAR(255),  -- Position innerhalb des Containers
                       qr_code VARCHAR(255) UNIQUE,
                       last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
                       version BIGINT NOT NULL DEFAULT 0,
                       user_id VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraint: Box muss genau einen Container haben
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

    -- Ein Item kann in verschiedenen Containern sein
                       box_id BIGINT REFERENCES boxes(id) ON DELETE SET NULL,
                       shelf_id BIGINT REFERENCES shelves(id) ON DELETE SET NULL,
                       room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,

                       position VARCHAR(255),  -- Position im Container
                       quantity INTEGER NOT NULL DEFAULT 1,

                       barcode VARCHAR(255),
                       qr_code VARCHAR(255) UNIQUE,

                       last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
                       version BIGINT NOT NULL DEFAULT 0,
                       user_id VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraint: Item muss irgendwo sein
                       CONSTRAINT item_location_check CHECK (
                           box_id IS NOT NULL OR shelf_id IS NOT NULL OR room_id IS NOT NULL
                           ),

                       CONSTRAINT quantity_positive CHECK (quantity > 0)
);

CREATE TABLE item_tags (
                           item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                           tag VARCHAR(100) NOT NULL,
                           PRIMARY KEY (item_id, tag)
);

-- ============================================================
-- Sync & Audit
-- ============================================================

CREATE TABLE sync_events (
                             id BIGSERIAL PRIMARY KEY,
                             event_id UUID UNIQUE NOT NULL,
                             client_id VARCHAR(255) NOT NULL,  -- Welches Gerät
                             user_id VARCHAR(255) NOT NULL,

                             event_type VARCHAR(50) NOT NULL,    -- CREATE, UPDATE, DELETE, MOVE
                             entity_type VARCHAR(50) NOT NULL,   -- ITEM, BOX, SHELF, ROOM
                             entity_id BIGINT NOT NULL,

                             data JSONB NOT NULL,  -- Event payload

                             timestamp TIMESTAMP NOT NULL,
                             vector_clock BIGINT NOT NULL,  -- Für Konflikt-Erkennung

                             status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPLIED, CONFLICT

                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),

                             CONSTRAINT check_event_type CHECK (event_type IN ('CREATE', 'UPDATE', 'DELETE', 'MOVE')),
                             CONSTRAINT check_entity_type CHECK (entity_type IN ('ITEM', 'BOX', 'SHELF', 'ROOM')),
                             CONSTRAINT check_status CHECK (status IN ('PENDING', 'APPLIED', 'CONFLICT'))
);

CREATE TABLE audit_log (
                           id BIGSERIAL PRIMARY KEY,
                           user_id VARCHAR(255) NOT NULL,
                           timestamp TIMESTAMP NOT NULL DEFAULT NOW(),

                           action VARCHAR(50) NOT NULL,        -- CREATE, UPDATE, DELETE, MOVE
                           entity_type VARCHAR(50) NOT NULL,   -- ITEM, BOX, SHELF, ROOM
                           entity_id BIGINT NOT NULL,
                           entity_name VARCHAR(255),

                           old_value JSONB,
                           new_value JSONB,

                           description TEXT  -- Human-readable description
);

-- ============================================================
-- Search & Tagging
-- ============================================================

CREATE TABLE synonyms (
                          id BIGSERIAL PRIMARY KEY,
                          canonical_term VARCHAR(255) NOT NULL,  -- Haupt-Begriff
                          synonym VARCHAR(255) NOT NULL,          -- Alternative
                          user_id VARCHAR(255),                   -- null = global
                          created_at TIMESTAMP DEFAULT NOW(),
                          UNIQUE(canonical_term, synonym)
);

CREATE TABLE tag_rules (
                           id BIGSERIAL PRIMARY KEY,
                           tag VARCHAR(100) NOT NULL,
                           rule_type VARCHAR(50) NOT NULL,  -- 'KEYWORD', 'REGEX', 'CUSTOM'
                           pattern TEXT NOT NULL,
                           priority INTEGER DEFAULT 0,
                           active BOOLEAN DEFAULT TRUE,
                           user_id VARCHAR(255),  -- null = globale Regel
                           created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================
-- Photos (für später)
-- ============================================================

CREATE TABLE photos (
                        id BIGSERIAL PRIMARY KEY,
                        item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,

                        s3_key VARCHAR(500) NOT NULL,
                        s3_url VARCHAR(1000),

                        is_primary BOOLEAN NOT NULL DEFAULT FALSE,

                        uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        user_id VARCHAR(255) NOT NULL
);

-- ============================================================
-- Indexes für Performance
-- ============================================================

-- Items
CREATE INDEX idx_items_box_id ON items(box_id);
CREATE INDEX idx_items_shelf_id ON items(shelf_id);
CREATE INDEX idx_items_room_id ON items(room_id);
CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_last_modified ON items(last_modified);
CREATE INDEX idx_items_name_trgm ON items USING gin(name gin_trgm_ops);  -- Fuzzy search

-- Boxes
CREATE INDEX idx_boxes_shelf_id ON boxes(shelf_id);
CREATE INDEX idx_boxes_room_id ON boxes(room_id);
CREATE INDEX idx_boxes_parent_box_id ON boxes(parent_box_id);
CREATE INDEX idx_boxes_user_id ON boxes(user_id);

-- Shelves
CREATE INDEX idx_shelves_room_id ON shelves(room_id);
CREATE INDEX idx_shelves_user_id ON shelves(user_id);

-- Rooms
CREATE INDEX idx_rooms_user_id ON rooms(user_id);
CREATE INDEX idx_rooms_type ON rooms(location_type);

-- Sync Events
CREATE INDEX idx_sync_events_user_id ON sync_events(user_id);
CREATE INDEX idx_sync_events_timestamp ON sync_events(timestamp);
CREATE INDEX idx_sync_events_status ON sync_events(status);
CREATE INDEX idx_sync_events_entity ON sync_events(entity_type, entity_id);

-- Audit Log
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);

-- Tags
CREATE INDEX idx_item_tags_tag ON item_tags(tag);

-- Synonyms
CREATE INDEX idx_synonyms_synonym ON synonyms(synonym);
CREATE INDEX idx_synonyms_canonical ON synonyms(canonical_term);

-- ============================================================
-- Initial Data
-- ============================================================

-- Beispiel-Synonyme
INSERT INTO synonyms (canonical_term, synonym) VALUES
                                                   ('Smartphone', 'Handy'),
                                                   ('Smartphone', 'Mobiltelefon'),
                                                   ('Laptop', 'Notebook'),
                                                   ('Laptop', 'Computer'),
                                                   ('Taschenlampe', 'Lampe'),
                                                   ('Taschenlampe', 'Leuchte'),
                                                   ('Schlafsack', 'Sleeping Bag'),
                                                   ('Zelt', 'Tent');

-- Beispiel Tag-Regeln
INSERT INTO tag_rules (tag, rule_type, pattern, priority) VALUES
                                                              ('Technik', 'KEYWORD', 'laptop,handy,smartphone,tablet,kabel,ladegerät,powerbank,usb,akku,batterie,kopfhörer', 10),
                                                              ('Outdoor', 'KEYWORD', 'zelt,schlafsack,isomatte,rucksack,wanderschuhe,taschenlampe,kompass,messer', 10),
                                                              ('Basteln', 'KEYWORD', 'schere,kleber,papier,karton,farbe,pinsel,stift,marker,buntstift,wolle', 10),
                                                              ('Spiele', 'KEYWORD', 'ball,frisbee,würfel,karten,brettspiel,puzzle,spielzeug,puppe', 10),
                                                              ('Küche', 'KEYWORD', 'teller,tasse,besteck,gabel,messer,löffel,topf,pfanne,becher', 10),
                                                              ('Erste-Hilfe', 'KEYWORD', 'pflaster,verband,schere,pinzette,desinfektionsmittel,salbe', 10),
                                                              ('Werkzeug', 'KEYWORD', 'hammer,schraubendreher,zange,säge,bohrmaschine,wasserwaage,maßband', 10);