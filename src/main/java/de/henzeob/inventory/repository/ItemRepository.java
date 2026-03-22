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

    public void insert(Item item) {
        this.getEntityManager().persist(item);
    }
}
