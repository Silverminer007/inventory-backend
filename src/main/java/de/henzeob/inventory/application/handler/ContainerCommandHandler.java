package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.CategoryService;
import de.henzeob.inventory.application.ContainerService;
import de.henzeob.inventory.exceptions.InvalidCommandPayloadException;
import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Image;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.ImageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static de.henzeob.inventory.application.handler.PayloadValidator.optionalName;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalString;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalUUID;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireCreatedAtBeforeNow;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireName;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireOnlyKeys;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireUUID;

@ApplicationScoped
public class ContainerCommandHandler {

    private static final Set<String> CREATE_REQUIRED = Set.of("id", "name", "parent", "created_at");
    private static final Set<String> CREATE_OPTIONAL = Set.of("description", "position", "category", "type");
    private static final Set<String> UPDATE_REQUIRED = Set.of("id");
    private static final Set<String> UPDATE_OPTIONAL =
            Set.of("name", "parent", "category", "description", "position", "primary_image", "type");
    private static final Set<String> DELETE_REQUIRED = Set.of("id");
    private static final Set<String> DELETE_OPTIONAL = Set.of();

    @Inject
    ContainerService containerService;

    @Inject
    CategoryService categoryService;

    @Inject
    ImageRepository imageRepository;

    @Inject
    Clock clock;

    public void handle(CommandType type, Map<String, Object> payload) {
        switch (type) {
            case CONTAINER_CREATE -> handleCreate(payload);
            case CONTAINER_UPDATE -> handleUpdate(payload);
            case CONTAINER_DELETE -> handleDelete(payload);
            default -> throw new IllegalArgumentException("Not a CONTAINER command: " + type);
        }
    }

    private void handleCreate(Map<String, Object> p) {
        requireOnlyKeys(p, CREATE_REQUIRED, CREATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        String name = requireName(p, "name");
        UUID parentId = requireUUID(p, "parent");
        LocalDateTime createdAt = requireCreatedAtBeforeNow(p, "created_at", clock);
        String description = optionalString(p, "description");
        String position = optionalString(p, "position");
        String type = optionalString(p, "type");
        UUID categoryId = optionalUUID(p, "category");

        Container parent = containerService.getExisting(parentId);
        Category category = categoryId != null ? categoryService.getExisting(categoryId) : null;

        Container container = containerService.create(id, name, description, parent, position, type, createdAt);
        container.category = category;
    }

    private void handleUpdate(Map<String, Object> p) {
        requireOnlyKeys(p, UPDATE_REQUIRED, UPDATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        if (id.equals(Container.ROOT_ID)) {
            throw new InvalidCommandPayloadException();
        }
        Container container = containerService.getExisting(id);

        String name = optionalName(p, "name");
        if (name != null) {
            container.name = name;
        }
        if (p.containsKey("description")) {
            container.description = optionalString(p, "description");
        }
        if (p.containsKey("position")) {
            container.position = optionalString(p, "position");
        }
        if (p.containsKey("type")) {
            container.type = optionalString(p, "type");
        }
        if (p.containsKey("parent")) {
            UUID parentId = requireUUID(p, "parent");
            Container newParent = containerService.getExisting(parentId);
            containerService.assertValidParent(id, newParent);
            container.parentContainer = newParent;
        }
        if (p.containsKey("category")) {
            UUID categoryId = optionalUUID(p, "category");
            container.category = categoryId != null ? categoryService.getExisting(categoryId) : null;
        }
        if (p.containsKey("primary_image")) {
            container.primaryImage = resolvePrimaryImage(p.get("primary_image"), container);
        }
    }

    private Image resolvePrimaryImage(Object rawValue, Container container) {
        if (rawValue == null) {
            return null;
        }
        UUID imageId;
        try {
            imageId = UUID.fromString(rawValue.toString());
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandPayloadException();
        }
        Image image = imageRepository.findByIdOptional(imageId)
                .orElseThrow(InvalidCommandPayloadException::new);
        if (image.container == null || !image.container.id.equals(container.id)) {
            throw new InvalidCommandPayloadException();
        }
        return image;
    }

    private void handleDelete(Map<String, Object> p) {
        requireOnlyKeys(p, DELETE_REQUIRED, DELETE_OPTIONAL);
        UUID id = requireUUID(p, "id");
        containerService.delete(id);
    }
}
