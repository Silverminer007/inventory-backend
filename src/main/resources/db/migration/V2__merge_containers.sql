-- V2: Merge rooms, shelves, boxes into a single containers table

-- ============================================================
-- 1. Create containers table
-- ============================================================

CREATE TABLE containers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    container_type VARCHAR(255) NOT NULL,  -- ROOM, SHELF, BOX
    parent_container_id BIGINT REFERENCES containers(id) ON DELETE CASCADE,
    location_type VARCHAR(255),  -- PERMANENT, TEMPORARY (only for ROOMs)
    location VARCHAR(255),      -- e.g. 'Keller', 'Ferienfreizeit 2025' (only for ROOMs)
    position VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Temporary columns for migration mapping
    old_id BIGINT,
    old_type VARCHAR(50),

    CONSTRAINT check_container_type CHECK (container_type IN ('ROOM', 'SHELF', 'BOX')),
    CONSTRAINT check_room_no_parent CHECK (
        container_type != 'ROOM' OR parent_container_id IS NULL
    ),
    CONSTRAINT check_location_type_room_only CHECK (
        location_type IS NULL OR container_type = 'ROOM'
    ),
    CONSTRAINT check_location_type_values CHECK (
        location_type IS NULL OR location_type IN ('PERMANENT', 'TEMPORARY')
    )
);

-- ============================================================
-- 2. Migrate rooms -> containers
-- ============================================================

INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT name, NULL, 'ROOM', NULL, location_type, location, position, qr_code, last_modified, version, user_id, created_at, id, 'ROOM'
FROM rooms;

-- ============================================================
-- 3. Migrate shelves -> containers
-- ============================================================

INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT s.name, NULL, 'SHELF', c.id, NULL, NULL, s.position, s.qr_code, s.last_modified, s.version, s.user_id, s.created_at, s.id, 'SHELF'
FROM shelves s
JOIN containers c ON c.old_id = s.room_id AND c.old_type = 'ROOM';

-- ============================================================
-- 4. Migrate boxes -> containers (iterative for nesting)
-- ============================================================

-- First: boxes directly in rooms (no parent_box, no shelf)
INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT b.name, b.description, 'BOX', c.id, NULL, NULL, b.position, b.qr_code, b.last_modified, b.version, b.user_id, b.created_at, b.id, 'BOX'
FROM boxes b
JOIN containers c ON c.old_id = b.room_id AND c.old_type = 'ROOM'
WHERE b.parent_box_id IS NULL AND b.shelf_id IS NULL AND b.room_id IS NOT NULL;

-- Second: boxes in shelves
INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT b.name, b.description, 'BOX', c.id, NULL, NULL, b.position, b.qr_code, b.last_modified, b.version, b.user_id, b.created_at, b.id, 'BOX'
FROM boxes b
JOIN containers c ON c.old_id = b.shelf_id AND c.old_type = 'SHELF'
WHERE b.parent_box_id IS NULL AND b.shelf_id IS NOT NULL;

-- Third: nested boxes (boxes in boxes) - handle up to 10 levels of nesting
-- Level 1: boxes whose parent_box has already been migrated
INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT b.name, b.description, 'BOX', c.id, NULL, NULL, b.position, b.qr_code, b.last_modified, b.version, b.user_id, b.created_at, b.id, 'BOX'
FROM boxes b
JOIN containers c ON c.old_id = b.parent_box_id AND c.old_type = 'BOX'
WHERE b.parent_box_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM containers WHERE old_id = b.id AND old_type = 'BOX');

-- Level 2+: repeat for deeper nesting
INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT b.name, b.description, 'BOX', c.id, NULL, NULL, b.position, b.qr_code, b.last_modified, b.version, b.user_id, b.created_at, b.id, 'BOX'
FROM boxes b
JOIN containers c ON c.old_id = b.parent_box_id AND c.old_type = 'BOX'
WHERE b.parent_box_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM containers WHERE old_id = b.id AND old_type = 'BOX');

INSERT INTO containers (name, description, container_type, parent_container_id, location_type, location, position, qr_code, last_modified, version, user_id, created_at, old_id, old_type)
SELECT b.name, b.description, 'BOX', c.id, NULL, NULL, b.position, b.qr_code, b.last_modified, b.version, b.user_id, b.created_at, b.id, 'BOX'
FROM boxes b
JOIN containers c ON c.old_id = b.parent_box_id AND c.old_type = 'BOX'
WHERE b.parent_box_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM containers WHERE old_id = b.id AND old_type = 'BOX');

-- ============================================================
-- 5. Update items to reference containers
-- ============================================================

ALTER TABLE items ADD COLUMN container_id BIGINT;

-- Items in boxes
UPDATE items SET container_id = c.id
FROM containers c
WHERE c.old_id = items.box_id AND c.old_type = 'BOX'
  AND items.box_id IS NOT NULL;

-- Items in shelves (only if not already assigned)
UPDATE items SET container_id = c.id
FROM containers c
WHERE c.old_id = items.shelf_id AND c.old_type = 'SHELF'
  AND items.shelf_id IS NOT NULL AND items.container_id IS NULL;

-- Items in rooms (only if not already assigned)
UPDATE items SET container_id = c.id
FROM containers c
WHERE c.old_id = items.room_id AND c.old_type = 'ROOM'
  AND items.room_id IS NOT NULL AND items.container_id IS NULL;

-- Make container_id NOT NULL and add FK
ALTER TABLE items ALTER COLUMN container_id SET NOT NULL;
ALTER TABLE items ADD CONSTRAINT fk_items_container FOREIGN KEY (container_id) REFERENCES containers(id) ON DELETE CASCADE;

-- Drop old columns from items
ALTER TABLE items DROP CONSTRAINT IF EXISTS item_location_check;
ALTER TABLE items DROP COLUMN box_id;
ALTER TABLE items DROP COLUMN shelf_id;
ALTER TABLE items DROP COLUMN room_id;

-- ============================================================
-- 6. Update sync_events entity_type constraint
-- ============================================================

ALTER TABLE sync_events DROP CONSTRAINT IF EXISTS check_entity_type;
ALTER TABLE sync_events ADD CONSTRAINT check_entity_type CHECK (entity_type IN ('ITEM', 'CONTAINER'));

-- Update existing sync_events
UPDATE sync_events SET entity_type = 'CONTAINER' WHERE entity_type IN ('BOX', 'SHELF', 'ROOM');

-- ============================================================
-- 7. Drop temporary migration columns
-- ============================================================

ALTER TABLE containers DROP COLUMN old_id;
ALTER TABLE containers DROP COLUMN old_type;

-- ============================================================
-- 8. Drop old tables
-- ============================================================

DROP TABLE IF EXISTS boxes CASCADE;
DROP TABLE IF EXISTS shelves CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;

-- ============================================================
-- 9. Create indexes for containers
-- ============================================================

CREATE INDEX idx_containers_user_id ON containers(user_id);
CREATE INDEX idx_containers_type ON containers(container_type);
CREATE INDEX idx_containers_parent ON containers(parent_container_id);
CREATE INDEX idx_containers_name_trgm ON containers USING gin(name gin_trgm_ops);
CREATE INDEX idx_items_container_id ON items(container_id);
