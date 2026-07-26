-- Rebuild the schema from scratch to match DOMAIN-RULES.md.
-- This is a pre-release/MVP codebase (see V9) - existing local data is not preserved.

DROP TABLE IF EXISTS sync_events CASCADE;
DROP TABLE IF EXISTS tag_rules CASCADE;
DROP TABLE IF EXISTS tag_suggestion_cache_tags CASCADE;
DROP TABLE IF EXISTS tag_suggestion_cache CASCADE;
DROP TABLE IF EXISTS item_tags CASCADE;
DROP TABLE IF EXISTS synonyms CASCADE;
DROP SEQUENCE IF EXISTS item_tags_SEQ;

DROP TABLE IF EXISTS photos CASCADE;
DROP TABLE IF EXISTS items CASCADE;
DROP TABLE IF EXISTS containers CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS commands CASCADE;

-- ============================================================
-- Categories
-- ============================================================

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    short_code VARCHAR(4) NOT NULL,
    hue INTEGER CHECK (hue IS NULL OR (hue BETWEEN 0 AND 360)),
    created_at TIMESTAMP NOT NULL
);

-- ============================================================
-- Containers
-- ============================================================

CREATE TABLE containers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_container_id UUID REFERENCES containers (id),
    category_id UUID REFERENCES categories (id),
    position VARCHAR(255),
    type VARCHAR(255),
    primary_image_id UUID,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_containers_parent ON containers (parent_container_id);
CREATE INDEX idx_containers_category ON containers (category_id);

-- ============================================================
-- Items
-- ============================================================

CREATE TABLE items (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    container_id UUID NOT NULL REFERENCES containers (id),
    category_id UUID REFERENCES categories (id),
    position VARCHAR(255),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    primary_image_id UUID,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_items_container ON items (container_id);
CREATE INDEX idx_items_category ON items (category_id);

-- ============================================================
-- Images (proxy metadata for S3-backed binary data)
-- ============================================================

CREATE TABLE images (
    id UUID PRIMARY KEY,
    item_id UUID REFERENCES items (id),
    container_id UUID REFERENCES containers (id),
    s3_key VARCHAR(500),
    content_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT images_owner_check CHECK (
        (item_id IS NOT NULL AND container_id IS NULL) OR
        (item_id IS NULL AND container_id IS NOT NULL)
        )
);

CREATE INDEX idx_images_item ON images (item_id);
CREATE INDEX idx_images_container ON images (container_id);

ALTER TABLE containers
    ADD CONSTRAINT fk_containers_primary_image FOREIGN KEY (primary_image_id) REFERENCES images (id);
ALTER TABLE items
    ADD CONSTRAINT fk_items_primary_image FOREIGN KEY (primary_image_id) REFERENCES images (id);

-- ============================================================
-- Commands (doubly-linked list; source of truth for all data)
-- ============================================================

CREATE TABLE commands (
    id                BIGSERIAL PRIMARY KEY,
    command_id        UUID        NOT NULL UNIQUE,
    parent_command_id UUID UNIQUE REFERENCES commands (command_id),
    child_command_id  UUID UNIQUE REFERENCES commands (command_id),
    command_type      VARCHAR(50) NOT NULL,
    command_version   INTEGER     NOT NULL DEFAULT 1,
    entity_id         UUID,
    payload           JSONB       NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    applied_at        TIMESTAMP
);

CREATE INDEX idx_commands_entity ON commands (entity_id);

-- ============================================================
-- Seed data: the ROOT container/command (fixed id, per DOMAIN-RULES.md).
-- Neither may be edited or deleted through the normal command API.
-- ============================================================

INSERT INTO containers (id, name, description, parent_container_id, category_id, position, type, primary_image_id, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Root Container', NULL, NULL, NULL, NULL, NULL, NULL,
        TIMESTAMP '1970-01-01 00:00:00');

INSERT INTO commands (command_id, parent_command_id, child_command_id, command_type, command_version, entity_id,
                       payload, created_at, applied_at)
VALUES ('11111111-1111-1111-1111-111111111111', NULL, NULL, 'CONTAINER_CREATE', 1,
        '11111111-1111-1111-1111-111111111111',
        '{"id": "11111111-1111-1111-1111-111111111111", "name": "Root Container", "created_at": "1970-01-01T00:00:00"}'::jsonb,
        TIMESTAMP '1970-01-01 00:00:00', TIMESTAMP '1970-01-01 00:00:00');
