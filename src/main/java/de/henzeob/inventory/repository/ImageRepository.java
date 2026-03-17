package de.henzeob.inventory.repository;

import de.henzeob.inventory.model.entity.Image;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ImageRepository implements PanacheRepository<Image> {

    public List<Image> findByItemAndUser(UUID itemId, String userId) {
        return list("item.id = ?1 and userId = ?2", Sort.by("uploadedAt"), itemId, userId);
    }

    public List<Image> findByContainerAndUser(UUID containerId, String userId) {
        return list("container.id = ?1 and userId = ?2", Sort.by("uploadedAt"), containerId, userId);
    }

    public Optional<Image> findByIdAndUser(UUID id, String userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public long countByItemAndUser(UUID itemId, String userId) {
        return count("item.id = ?1 and userId = ?2", itemId, userId);
    }

    public long countByContainerAndUser(UUID containerId, String userId) {
        return count("container.id = ?1 and userId = ?2", containerId, userId);
    }
}
