-- V3: Add indexes for efficient synonym lookups

-- Index for user-scoped queries
CREATE INDEX IF NOT EXISTS idx_synonyms_user_id ON synonyms(user_id);

-- Case-insensitive indexes for bidirectional synonym lookup
CREATE INDEX idx_synonyms_canonical_lower ON synonyms(LOWER(canonical_term));
CREATE INDEX idx_synonyms_synonym_lower ON synonyms(LOWER(synonym));
