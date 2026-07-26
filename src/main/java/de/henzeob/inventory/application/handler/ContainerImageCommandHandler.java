package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.ContainerService;
import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.enums.CommandType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static de.henzeob.inventory.application.handler.PayloadValidator.requireCreatedAtBeforeNow;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireOnlyKeys;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireUUID;

@ApplicationScoped
public class ContainerImageCommandHandler {

    private static final Set<String> CREATE_REQUIRED = Set.of("id", "container", "created_at");
    private static final Set<String> CREATE_OPTIONAL = Set.of();
    private static final Set<String> DELETE_REQUIRED = Set.of("id");
    private static final Set<String> DELETE_OPTIONAL = Set.of();

    @Inject
    ImageService imageService;

    @Inject
    ContainerService containerService;

    @Inject
    Clock clock;

    public void handle(CommandType type, Map<String, Object> payload) {
        switch (type) {
            case CONTAINER_IMAGE_CREATE -> handleCreate(payload);
            case CONTAINER_IMAGE_DELETE -> handleDelete(payload);
            default -> throw new IllegalArgumentException("Not a CONTAINER_IMAGE command: " + type);
        }
    }

    private void handleCreate(Map<String, Object> p) {
        requireOnlyKeys(p, CREATE_REQUIRED, CREATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        UUID containerId = requireUUID(p, "container");
        LocalDateTime createdAt = requireCreatedAtBeforeNow(p, "created_at", clock);

        Container container = containerService.getExisting(containerId);
        imageService.createForContainer(id, container, createdAt);
    }

    private void handleDelete(Map<String, Object> p) {
        requireOnlyKeys(p, DELETE_REQUIRED, DELETE_OPTIONAL);
        UUID id = requireUUID(p, "id");
        var image = imageService.getExisting(id);
        if (image.container == null) {
            throw new IllegalArgumentException("Not a container image: " + id);
        }
        imageService.delete(id);
    }
}
