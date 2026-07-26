package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.ImageService;
import de.henzeob.inventory.application.ItemService;
import de.henzeob.inventory.model.entity.Item;
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
public class ItemImageCommandHandler {

    private static final Set<String> CREATE_REQUIRED = Set.of("id", "item", "created_at");
    private static final Set<String> CREATE_OPTIONAL = Set.of();
    private static final Set<String> DELETE_REQUIRED = Set.of("id");
    private static final Set<String> DELETE_OPTIONAL = Set.of();

    @Inject
    ImageService imageService;

    @Inject
    ItemService itemService;

    @Inject
    Clock clock;

    public void handle(CommandType type, Map<String, Object> payload) {
        switch (type) {
            case ITEM_IMAGE_CREATE -> handleCreate(payload);
            case ITEM_IMAGE_DELETE -> handleDelete(payload);
            default -> throw new IllegalArgumentException("Not an ITEM_IMAGE command: " + type);
        }
    }

    private void handleCreate(Map<String, Object> p) {
        requireOnlyKeys(p, CREATE_REQUIRED, CREATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        UUID itemId = requireUUID(p, "item");
        LocalDateTime createdAt = requireCreatedAtBeforeNow(p, "created_at", clock);

        Item item = itemService.getExisting(itemId);
        imageService.createForItem(id, item, createdAt);
    }

    private void handleDelete(Map<String, Object> p) {
        requireOnlyKeys(p, DELETE_REQUIRED, DELETE_OPTIONAL);
        UUID id = requireUUID(p, "id");
        var image = imageService.getExisting(id);
        if (image.item == null) {
            throw new IllegalArgumentException("Not an item image: " + id);
        }
        imageService.delete(id);
    }
}
