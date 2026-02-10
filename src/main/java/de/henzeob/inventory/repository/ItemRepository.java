package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Item;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ItemRepository implements PanacheRepository<Item> {

    /**
     * Find all items for a specific user
     */
    public List<Item> findByUser(String userId) {
        return list("userId", Sort.by("name"), userId);
    }

    /**
     * Find item by ID and user (security check)
     */
    public Optional<Item> findByIdAndUser(Long id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    /**
     * Search items by name (case-insensitive)
     */
    public List<Item> searchByName(String query, String userId) {
        return list("LOWER(name) LIKE LOWER(?1) and userId = ?2",
                "%" + query + "%", userId);
    }

    /**
     * Fuzzy search using PostgreSQL trigram similarity
     */
    public List<Item> fuzzySearch(String query, String userId, double threshold) {
        return getEntityManager()
                .createNativeQuery("""
                SELECT i.*
                FROM items i
                WHERE i.user_id = :userId
                  AND (
                    i.name % :query
                    OR COALESCE(i.description, '') % :query
                  )
                  AND GREATEST(
                    similarity(i.name, :query),
                    similarity(COALESCE(i.description, ''), :query)
                  ) >= :threshold
                ORDER BY GREATEST(
                  similarity(i.name, :query),
                  similarity(COALESCE(i.description, ''), :query)
                ) DESC
                LIMIT 20
            """, Item.class)
                .setParameter("query", query)
                .setParameter("userId", userId)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    /**
     * Find items by tag
     */
    public List<Item> findByTag(String tag, String userId) {
        return getEntityManager()
                .createQuery("""
                SELECT i FROM Item i
                JOIN i.tags t
                WHERE t = :tag AND i.userId = :userId
                ORDER BY i.name
            """, Item.class)
                .setParameter("tag", tag)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Find items in a specific container
     */
    public List<Item> findByContainer(Long containerId, String userId) {
        return list("container.id = ?1 and userId = ?2", Sort.by("name"), containerId, userId);
    }

    /**
     * Find all items in a container tree (container + all descendants) using recursive CTE
     */
    @SuppressWarnings("unchecked")
    public List<Item> findAllInContainerTree(Long containerId, String userId) {
        return getEntityManager()
                .createNativeQuery("""
                WITH RECURSIVE descendants AS (
                    SELECT id FROM containers WHERE id = :containerId AND user_id = :userId
                    UNION ALL
                    SELECT c.id FROM containers c
                    JOIN descendants d ON c.parent_container_id = d.id
                )
                SELECT i.* FROM items i
                WHERE i.container_id IN (SELECT id FROM descendants)
                  AND i.user_id = :userId
                ORDER BY i.name
            """, Item.class)
                .setParameter("containerId", containerId)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Find all distinct tags used by a user's items
     */
    @SuppressWarnings("unchecked")
    public List<String> findDistinctTagsByUser(String userId) {
        return getEntityManager()
                .createNativeQuery("""
                    SELECT DISTINCT t.tag
                    FROM item_tags t
                    JOIN items i ON t.item_id = i.id
                    WHERE i.user_id = :userId
                    ORDER BY t.tag
                """)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Count items by user
     */
    public long countByUser(String userId) {
        return count("userId", userId);
    }

    /**
     * Find items modified since timestamp (for sync)
     */
    public List<Item> findModifiedSince(String userId, java.time.LocalDateTime since) {
        return list("userId = ?1 and lastModified > ?2",
                Sort.by("lastModified"), userId, since);
    }
}
