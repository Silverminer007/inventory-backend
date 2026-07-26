package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.CategoryService;
import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.model.enums.CommandType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static de.henzeob.inventory.application.handler.PayloadValidator.optionalHue;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalName;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalShortCode;
import static de.henzeob.inventory.application.handler.PayloadValidator.optionalString;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireCreatedAtBeforeNow;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireName;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireOnlyKeys;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireShortCode;
import static de.henzeob.inventory.application.handler.PayloadValidator.requireUUID;

@ApplicationScoped
public class CategoryCommandHandler {

    private static final Set<String> CREATE_REQUIRED = Set.of("id", "name", "shortcode", "created_at");
    private static final Set<String> CREATE_OPTIONAL = Set.of("description", "hue");
    private static final Set<String> UPDATE_REQUIRED = Set.of("id");
    private static final Set<String> UPDATE_OPTIONAL = Set.of("name", "shortcode", "description", "hue");
    private static final Set<String> DELETE_REQUIRED = Set.of("id");
    private static final Set<String> DELETE_OPTIONAL = Set.of();

    @Inject
    CategoryService categoryService;

    @Inject
    Clock clock;

    public void handle(CommandType type, Map<String, Object> payload) {
        switch (type) {
            case CATEGORY_CREATE -> handleCreate(payload);
            case CATEGORY_UPDATE -> handleUpdate(payload);
            case CATEGORY_DELETE -> handleDelete(payload);
            default -> throw new IllegalArgumentException("Not a CATEGORY command: " + type);
        }
    }

    private void handleCreate(Map<String, Object> p) {
        requireOnlyKeys(p, CREATE_REQUIRED, CREATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        String name = requireName(p, "name");
        String shortCode = requireShortCode(p, "shortcode");
        LocalDateTime createdAt = requireCreatedAtBeforeNow(p, "created_at", clock);
        String description = optionalString(p, "description");
        Integer hue = optionalHue(p, "hue");

        categoryService.create(id, name, description, shortCode, hue, createdAt);
    }

    private void handleUpdate(Map<String, Object> p) {
        requireOnlyKeys(p, UPDATE_REQUIRED, UPDATE_OPTIONAL);

        UUID id = requireUUID(p, "id");
        Category category = categoryService.getExisting(id);

        String name = optionalName(p, "name");
        if (name != null) {
            category.name = name;
        }
        String shortCode = optionalShortCode(p, "shortcode");
        if (shortCode != null) {
            category.shortCode = shortCode;
        }
        if (p.containsKey("description")) {
            category.description = optionalString(p, "description");
        }
        Integer hue = optionalHue(p, "hue");
        if (p.containsKey("hue")) {
            category.hue = hue;
        }
    }

    private void handleDelete(Map<String, Object> p) {
        requireOnlyKeys(p, DELETE_REQUIRED, DELETE_OPTIONAL);
        UUID id = requireUUID(p, "id");
        categoryService.delete(id);
    }
}
