package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.repository.ContainerRepository;
import de.henzeob.inventory.repository.ImageRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ContainerService {

    @Inject
    ContainerRepository containerRepository;

    @Inject
    ItemRepository itemRepository;

    @Inject
    ImageRepository imageRepository;

    public Container getExisting(UUID id) {
        return containerRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + id));
    }

    public Container create(UUID id, String name, String description, Container parent, String position,
                             String type, LocalDateTime createdAt) {
        if (id.equals(parent.id)) {
            throw new IllegalArgumentException("Container cannot be its own parent: " + id);
        }
        Container container = new Container();
        container.id = id;
        container.name = name;
        container.description = description;
        container.parentContainer = parent;
        container.position = position;
        container.type = type;
        container.createdAt = createdAt;
        containerRepository.persist(container);
        return container;
    }

    /**
     * Ensures assigning newParent as the parent of container would not create a cycle
     * (newParent is not container itself, nor a descendant of container).
     */
    public void assertValidParent(UUID containerId, Container newParent) {
        if (containerId.equals(newParent.id)) {
            throw new IllegalArgumentException("Container cannot be its own parent: " + containerId);
        }
        Container current = newParent;
        int depth = 0;
        while (current.parentContainer != null && depth < 1000) {
            if (current.parentContainer.id.equals(containerId)) {
                throw new IllegalArgumentException("Circular container reference detected for: " + containerId);
            }
            current = current.parentContainer;
            depth++;
        }
    }

    public void delete(UUID id) {
        if (id.equals(Container.ROOT_ID)) {
            throw new IllegalArgumentException("The root container may not be deleted");
        }
        Container container = getExisting(id);
        if (containerRepository.existsByParent(id)
                || itemRepository.existsByContainer(id)
                || imageRepository.existsByContainer(id)) {
            throw new IllegalArgumentException("Container is still referenced by a child container, item, or image: " + id);
        }
        containerRepository.delete(container);
    }
}
