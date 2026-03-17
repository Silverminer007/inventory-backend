-- V9: Migrate primary keys from BIGINT to UUID for offline client ID generation
-- Note: This migration drops and recreates affected tables. Existing data is not preserved.
-- This is intentional for the development/MVP phase of this project.

-- ============================================================
-- 1. Drop tables in dependency order (most-dependent first)
-- ============================================================

DROP TABLE IF EXISTS photos CASCADE;
DROP TABLE IF EXISTS item_tags CASCADE;
DROP TABLE IF EXISTS items CASCADE;
DROP TABLE IF EXISTS containers CASCADE;
DROP TABLE IF EXISTS synonyms CASCADE;

DROP SEQUENCE IF EXISTS item_tags_SEQ;

-- ============================================================
-- 2. Recreate containers with UUID PK
-- ============================================================

CREATE TABLE containers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    container_type VARCHAR(255) NOT NULL,
    parent_container_id UUID REFERENCES containers(id) ON DELETE CASCADE,
    location_type VARCHAR(255),
    location VARCHAR(255),
    position VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
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
-- 3. Recreate items with UUID PK
-- ============================================================

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    container_id UUID NOT NULL REFERENCES containers(id) ON DELETE CASCADE,
    position VARCHAR(255),
    quantity INTEGER NOT NULL DEFAULT 1,
    barcode VARCHAR(255),
    qr_code VARCHAR(255) UNIQUE,
    last_modified TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT quantity_positive CHECK (quantity > 0)
);

-- ============================================================
-- 4. Recreate item_tags (BIGINT id stays, item_id becomes UUID)
-- ============================================================

CREATE TABLE item_tags (
    id BIGINT NOT NULL,
    tag VARCHAR(255),
    tag_type VARCHAR(255),
    item_id UUID REFERENCES items(id) ON DELETE CASCADE,
    UNIQUE (item_id, tag)
);

CREATE SEQUENCE item_tags_SEQ START WITH 1 INCREMENT BY 50;

-- ============================================================
-- 5. Recreate photos with UUID PK
-- ============================================================

CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID REFERENCES items(id) ON DELETE CASCADE,
    container_id UUID REFERENCES containers(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    s3_url VARCHAR(1000),
    filename VARCHAR(255),
    content_type VARCHAR(100),
    file_size BIGINT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id VARCHAR(255) NOT NULL,
    CONSTRAINT photos_owner_check CHECK (
        (item_id IS NOT NULL AND container_id IS NULL) OR
        (item_id IS NULL AND container_id IS NOT NULL)
    )
);

-- ============================================================
-- 6. Recreate synonyms with UUID PK
-- ============================================================

CREATE TABLE synonyms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_term VARCHAR(255) NOT NULL,
    synonym VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(canonical_term, synonym)
);

-- ============================================================
-- 7. Migrate commands.entity_id from BIGINT to UUID
-- ============================================================

DROP INDEX IF EXISTS idx_commands_entity;
ALTER TABLE commands DROP COLUMN entity_id;
ALTER TABLE commands ADD COLUMN entity_id UUID;
CREATE INDEX idx_commands_entity ON commands(entity_type, entity_id);

-- ============================================================
-- 8. Recreate indexes
-- ============================================================

CREATE INDEX idx_containers_user_id ON containers(user_id);
CREATE INDEX idx_containers_type ON containers(container_type);
CREATE INDEX idx_containers_parent ON containers(parent_container_id);
CREATE INDEX idx_containers_name_trgm ON containers USING gin(name gin_trgm_ops);

CREATE INDEX idx_items_container_id ON items(container_id);
CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_last_modified ON items(last_modified);
CREATE INDEX idx_items_name_trgm ON items USING gin(name gin_trgm_ops);

CREATE INDEX idx_item_tags_tag ON item_tags(tag);

CREATE INDEX idx_photos_item_id ON photos(item_id);
CREATE INDEX idx_photos_container_id ON photos(container_id);
CREATE INDEX idx_photos_user_id ON photos(user_id);

CREATE INDEX idx_synonyms_synonym ON synonyms(synonym);
CREATE INDEX idx_synonyms_canonical ON synonyms(canonical_term);

-- ============================================================
-- 9. Re-insert initial synonym data
-- ============================================================

INSERT INTO synonyms (canonical_term, synonym) VALUES
    ('Smartphone', 'Handy'),
    ('Smartphone', 'Mobiltelefon'),
    ('Laptop', 'Notebook'),
    ('Laptop', 'Computer'),
    ('Taschenlampe', 'Lampe'),
    ('Taschenlampe', 'Leuchte'),
    ('Schlafsack', 'Sleeping Bag'),
    ('Zelt', 'Tent');
