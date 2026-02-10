CREATE TABLE tag_suggestion_cache (
    id BIGSERIAL PRIMARY KEY,
    input_text TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tag_suggestion_cache_tags (
    cache_id BIGINT NOT NULL REFERENCES tag_suggestion_cache(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (cache_id, tag)
);
