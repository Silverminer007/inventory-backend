CREATE TABLE categories
(
    id            UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(255)     NOT NULL,
    description   VARCHAR(255),
    short_code    VARCHAR(255)     NOT NULL UNIQUE,
    created_at    TIMESTAMP        NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP        NOT NULL DEFAULT NOW(),
    version       BIGINT           NOT NULL DEFAULT 0
);

CREATE INDEX categories_name_index ON categories (name);

INSERT INTO categories (name, description, short_code)
VALUES ('Sonstiges',
        'Alles was in keine andere Kategorie passt. Wenn sich mehrere Gegenstände zusammenfassen lassen, sollte eine neue Kategorie erstellt werden',
        'XX');

ALTER TABLE items ADD COLUMN category_id UUID REFERENCES categories (id);
UPDATE items SET category_id = (SELECT id FROM categories WHERE short_code = 'XX');
ALTER TABLE items ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE containers ADD COLUMN category_id UUID REFERENCES categories (id);
UPDATE containers SET category_id = (SELECT id FROM categories WHERE short_code = 'XX');
ALTER TABLE containers ALTER COLUMN category_id SET NOT NULL;

-- Insert an APPLIED CATEGORY_CREATE command for every seeded category so that
-- clients receive them through the normal command-sync endpoint.
-- userId = 'system' marks globally-owned commands (not tied to any single user).
INSERT INTO commands (command_id, command_type, payload_version, entity_type, entity_id, payload, user_id, status, applied_at, created_at)
SELECT gen_random_uuid(),
       'CATEGORY_CREATE',
       1,
       'CATEGORY',
       id,
       jsonb_build_object('id', id, 'name', name, 'shortCode', short_code, 'description', description),
       'system',
       'APPLIED',
       created_at,
       created_at
FROM categories;