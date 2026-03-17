package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Item;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ItemRepository implements PanacheRepository<Item> {

    public List<Item> findByUser(String userId) {
        return list("userId", Sort.by("name"), userId);
    }

    public Optional<Item> findByIdAndUser(UUID id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public List<Item> searchByName(String query, List<String> tags, String userId) {
        if (tags == null || tags.isEmpty()) {
            String sql = """
                    SELECT i FROM Item i
                    WHERE (LOWER(i.name) LIKE LOWER(?1)
                        OR LOWER(i.description) LIKE LOWER(?1))
                    AND i.userId = ?2
                    """;
            return list(sql, "%" + query + "%", userId);
        }
        String sql = """
                SELECT DISTINCT i FROM Item i
                JOIN ItemTag t ON i.id  = t.item.id
                WHERE
                (LOWER(i.name) LIKE LOWER(?1)
                OR LOWER(i.description) LIKE LOWER(?1))
                AND t.tag IN ?2
                AND i.userId = ?3
                """;
        return list(sql, "%" + query + "%", tags, userId);
    }

    /**
     * Fuzzy search using PostgreSQL trigram similarity
     */
    @SuppressWarnings("unchecked")
    public List<Item> fuzzySearch(String query, String userId, double threshold) {
        List<UUID> itemIds = getEntityManager()
                .createNativeQuery("""
                            SELECT i.id
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
                        """, UUID.class)
                .setParameter("query", query)
                .setParameter("userId", userId)
                .setParameter("threshold", threshold)
                .getResultList();

        return list("id IN ?1", itemIds);
    }

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

    public List<Item> findByContainer(UUID containerId, String userId) {
        return list("container.id = ?1 and userId = ?2", Sort.by("name"), containerId, userId);
    }

    /**
     * Find all items in a container tree (container + all descendants) using recursive CTE
     */
    @SuppressWarnings("unchecked")
    public List<Item> findAllInContainerTree(UUID containerId, String userId) {
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

    public long countByUser(String userId) {
        return count("userId", userId);
    }

    public List<Item> findModifiedSince(String userId, java.time.LocalDateTime since) {
        return list("userId = ?1 and lastModified > ?2",
                Sort.by("lastModified"), userId, since);
    }
}
