package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.CategoryService;
import de.henzeob.inventory.application.ContainerService;
import de.henzeob.inventory.application.ItemService;
import de.henzeob.inventory.exceptions.InvalidCommandPayloadException;
import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Image;
import de.henzeob.inventory.model.entity.Item;
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
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalQuantity;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalString;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalUUID;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireCreatedAtBeforeNow;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireName;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireOnlyKeys;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireQuantity;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireUUID;

@ApplicationScoped
public class ItemCommandHandler {

    private static final Set<String> CREATE_REQUIRED = Set.of("id", "name", "container", "quantity", "created_at");
    private static final Set<String> CREATE_OPTIONAL = Set.of("description", "position", "category");
    private static final Set<String> UPDATE_REQUIRED = Set.of("id");
    private static final Set<String> UPDATE_OPTIONAL =
            Set.of("name", "container", "category", "quantity", "description", "position", "primary_image");
    private static final Set<String> DELETE_REQUIRED = Set.of("id");
    private static final Set<String> DELETE_OPTIONAL = Set.of();

    @Inject
    ItemService itemService;

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
            case ITEM_CREATE -> handleCreate(payload);
            case ITEM_UPDATE -> handleUpdate(payload);
            case ITEM_DELETE -> handleDelete(payload);
            default -> throw new IllegalArgumentException("Not an ITEM command: " + type);
        }
    }

    private void handleCreate(Map<String, Object> p) {
        requireOnlyKeys(p, CREATE_REQUIRED, CREATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        String name = requireName(p, "name");
        UUID containerId = requireUUID(p, "container");
        int quantity = requireQuantity(p, "quantity");
        LocalDateTime createdAt = requireCreatedAtBeforeNow(p, "created_at", clock);
        String description = optionalString(p, "description");
        String position = optionalString(p, "position");
        UUID categoryId = optionalUUID(p, "category");

        Container container = containerService.getExisting(containerId);
        Category category = categoryId != null ? categoryService.getExisting(categoryId) : null;

        Item item = itemService.create(id, name, description, container, position, quantity, createdAt);
        item.category = category;
    }

    private void handleUpdate(Map<String, Object> p) {
        requireOnlyKeys(p, UPDATE_REQUIRED, UPDATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        Item item = itemService.getExisting(id);

        String name = optionalName(p, "name");
        if (name != null) {
            item.name = name;
        }
        if (p.containsKey("description")) {
            item.description = optionalString(p, "description");
        }
        if (p.containsKey("position")) {
            item.position = optionalString(p, "position");
        }
        Integer quantity = optionalQuantity(p, "quantity");
        if (quantity != null) {
            item.quantity = quantity;
        }
        if (p.containsKey("container")) {
            UUID containerId = requireUUID(p, "container");
            item.container = containerService.getExisting(containerId);
        }
        if (p.containsKey("category")) {
            UUID categoryId = optionalUUID(p, "category");
            item.category = categoryId != null ? categoryService.getExisting(categoryId) : null;
        }
        if (p.containsKey("primary_image")) {
            item.primaryImage = resolvePrimaryImage(p.get("primary_image"), item);
        }
    }

    private Image resolvePrimaryImage(Object rawValue, Item item) {
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
        if (image.item == null || !image.item.id.equals(item.id)) {
            throw new InvalidCommandPayloadException();
        }
        return image;
    }

    private void handleDelete(Map<String, Object> p) {
        requireOnlyKeys(p, DELETE_REQUIRED, DELETE_OPTIONAL);
        UUID id = requireUUID(p, "id");
        itemService.delete(id);
    }
}
