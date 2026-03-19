CREATE TABLE item_synonym
(
    item_id UUID NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    synonym TEXT NOT NULL,
    PRIMARY KEY (item_id, synonym)
);