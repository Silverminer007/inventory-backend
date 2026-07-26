package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Image;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ImageRepository implements PanacheRepositoryBase<Image, UUID> {

    public boolean existsByItem(UUID itemId) {
        return count("item.id = ?1", itemId) > 0;
    }

    public boolean existsByContainer(UUID containerId) {
        return count("container.id = ?1", containerId) > 0;
    }
}
