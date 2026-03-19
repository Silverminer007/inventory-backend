package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Synonym;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SynonymRepository implements PanacheRepository<Synonym> {

    /**
     * Find all synonyms for a given term (bidirectional lookup).
     * If the term matches a canonical_term, return all its synonyms.
     * If the term matches a synonym, return the canonical_term and its siblings.
     */
    public List<Synonym> findSynonymsForTerm(String term, String userId) {
        return list(
                "(LOWER(canonicalTerm) = LOWER(?1) OR LOWER(synonym) = LOWER(?1)) AND (userId IS NULL OR userId = ?2)",
                term, userId);
    }

    /**
     * Count global synonyms (userId IS NULL) for idempotency checks.
     */
    public long countGlobal() {
        return count("userId IS NULL");
    }

    /**
     * Check if a synonym pair already exists for a given user (or globally).
     */
    public boolean existsPair(String canonicalTerm, String synonym, String userId) {
        if (userId == null) {
            return count("LOWER(canonicalTerm) = LOWER(?1) AND LOWER(synonym) = LOWER(?2) AND userId IS NULL",
                    canonicalTerm, synonym) > 0;
        }
        return count("LOWER(canonicalTerm) = LOWER(?1) AND LOWER(synonym) = LOWER(?2) AND userId = ?3",
                canonicalTerm, synonym, userId) > 0;
    }
}
