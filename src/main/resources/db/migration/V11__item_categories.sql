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

CREATE UNIQUE INDEX categories_short_code_uindex ON categories (short_code);
CREATE INDEX categories_name_index ON categories (name);

INSERT INTO categories (name, description, short_code)
VALUES ('Sonstiges',
        'Alles was in keine andere Kategorie passt. Wenn sich mehrere Gegenstände zusammenfassen lassen, sollte eine neue Kategorie erstellt werden',
        'XX');

ALTER TABLE items
    ADD COLUMN category_id UUID REFERENCES categories (id) DEFAULT (SELECT id FROM categories WHERE short_code = 'XX');

ALTER TABLE containers
    ADD COLUMN category_id UUID REFERENCES categories (id) DEFAULT (SELECT id FROM categories WHERE short_code = 'XX');