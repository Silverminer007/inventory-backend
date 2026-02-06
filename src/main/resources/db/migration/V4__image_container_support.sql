-- V4: Extend photos table to support container images and add metadata

-- Make item_id nullable (photos can now belong to containers)
ALTER TABLE photos ALTER COLUMN item_id DROP NOT NULL;

-- Add container_id reference
ALTER TABLE photos ADD COLUMN container_id BIGINT REFERENCES containers(id) ON DELETE CASCADE;

-- Add file metadata
ALTER TABLE photos ADD COLUMN filename VARCHAR(255);
ALTER TABLE photos ADD COLUMN content_type VARCHAR(100);
ALTER TABLE photos ADD COLUMN file_size BIGINT;

-- Exactly one of item_id or container_id must be set
ALTER TABLE photos ADD CONSTRAINT photos_owner_check CHECK (
    (item_id IS NOT NULL AND container_id IS NULL) OR
    (item_id IS NULL AND container_id IS NOT NULL)
);

-- Indexes
CREATE INDEX idx_photos_container_id ON photos(container_id);
CREATE INDEX idx_photos_item_id ON photos(item_id);
CREATE INDEX idx_photos_user_id ON photos(user_id);
