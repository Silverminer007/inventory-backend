package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Container;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ContainerRepository implements PanacheRepositoryBase<Container, UUID> {

    public boolean existsByParent(UUID parentContainerId) {
        return count("parentContainer.id = ?1", parentContainerId) > 0;
    }

    public boolean existsByCategory(UUID categoryId) {
        return count("category.id = ?1", categoryId) > 0;
    }

    public boolean existsByPrimaryImage(UUID imageId) {
        return count("primaryImage.id = ?1", imageId) > 0;
    }
}
