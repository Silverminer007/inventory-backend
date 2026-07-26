package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Item;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ItemRepository implements PanacheRepositoryBase<Item, UUID> {

    public boolean existsByContainer(UUID containerId) {
        return count("container.id = ?1", containerId) > 0;
    }

    public boolean existsByCategory(UUID categoryId) {
        return count("category.id = ?1", categoryId) > 0;
    }

    public boolean existsByPrimaryImage(UUID imageId) {
        return count("primaryImage.id = ?1", imageId) > 0;
    }
}
