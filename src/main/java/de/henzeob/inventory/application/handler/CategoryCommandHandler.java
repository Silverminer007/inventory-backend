package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.CategoryService;
import de.henzeob.inventory.mapper.CategoryMapper;
import de.henzeob.inventory.model.dto.CategoryDTO;
import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.CategoryRepository;
import de.henzeob.inventory.repository.CommandRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CategoryCommandHandler {

    @Inject
    CategoryService categoryService;

    @Inject
    CategoryMapper categoryMapper;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CommandRepository commandRepository;

    public Object handle(CommandType type, Command command, String userId) {
        Map<String, Object> p = command.payload;
        return switch (type) {
            case CATEGORY_CREATE -> handleCreate(p);
            case CATEGORY_UPDATE -> handleUpdate(command.entityId, p);
            case CATEGORY_DELETE -> handleDelete(command.entityId, p);
            default -> throw new IllegalArgumentException("Not a CATEGORY command: " + type);
        };
    }

    private CategoryDTO handleCreate(Map<String, Object> p) {
        CategoryDTO dto = new CategoryDTO();
        if (p.get("id") != null) dto.id = toUUID(p.get("id")); // optional client-provided UUID
        dto.name = required(p, "name");
        dto.description = (String) p.get("description");
        dto.shortCode = required(p, "shortCode");
        return categoryService.createCategory(dto);
    }

    private Object handleUpdate(UUID entityId, Map<String, Object> p) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        Category category = categoryRepository.findByIdOptional(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + entityId));

        if (force || clientVersion == null || category.version.equals(clientVersion)) {
            return applyUpdate(entityId, p);
        }

        long versionGap = category.version - clientVersion;
        Set<String> serverChanged = serverChangedCategoryFields(entityId, versionGap);

        List<String> conflictingFields = new ArrayList<>();

        if (p.containsKey("name") && !Objects.equals(p.get("name"), category.name)
                && serverChanged.contains("name")) {
            conflictingFields.add("name");
        }
        if (p.containsKey("description") && !Objects.equals(p.get("description"), category.description)
                && serverChanged.contains("description")) {
            conflictingFields.add("description");
        }
        if (p.containsKey("shortCode") && !Objects.equals(p.get("shortCode"), category.shortCode)
                && serverChanged.contains("shortCode")) {
            conflictingFields.add("shortCode");
        }

        if (!conflictingFields.isEmpty()) {
            ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
            info.clientVersion = clientVersion;
            info.serverVersion = category.version;
            info.conflictingFields = conflictingFields;
            info.serverSnapshot = categoryMapper.toDTO(category);
            info.clientPayload = p;
            return new ConflictResult.Conflicted(info);
        }

        // Auto-merge
        CategoryDTO overlayDto = categoryMapper.toDTO(category);
        if (p.containsKey("name"))        overlayDto.name = (String) p.get("name");
        if (p.containsKey("description")) overlayDto.description = (String) p.get("description");
        if (p.containsKey("shortCode"))   overlayDto.shortCode = (String) p.get("shortCode");
        return categoryService.updateCategory(entityId, overlayDto);
    }

    private Set<String> serverChangedCategoryFields(UUID entityId, long versionGap) {
        if (versionGap <= 0) return Set.of();
        List<Command> recent = commandRepository.findRecentApplied(
                entityId, "CATEGORY", (int) Math.min(versionGap, 100));
        Set<String> meta = Set.of("version", "force");
        Set<String> changed = new HashSet<>();
        for (Command cmd : recent) {
            if (cmd.commandType == CommandType.CATEGORY_UPDATE && cmd.payload != null) {
                for (String key : cmd.payload.keySet()) {
                    if (!meta.contains(key)) changed.add(key);
                }
            }
        }
        return changed;
    }

    private CategoryDTO applyUpdate(UUID entityId, Map<String, Object> p) {
        CategoryDTO dto = new CategoryDTO();
        dto.id = entityId;
        dto.name = (String) p.get("name");
        dto.description = (String) p.get("description");
        dto.shortCode = (String) p.get("shortCode");
        dto.version = toLong(p.get("version"));
        return categoryService.updateCategory(entityId, dto);
    }

    private Object handleDelete(UUID entityId, Map<String, Object> p) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        if (!force && clientVersion != null) {
            Category category = categoryRepository.findByIdOptional(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + entityId));
            if (category.version > clientVersion) {
                ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
                info.clientVersion = clientVersion;
                info.serverVersion = category.version;
                info.conflictingFields = List.of();
                info.serverSnapshot = categoryMapper.toDTO(category);
                info.clientPayload = p;
                return new ConflictResult.Conflicted(info);
            }
        }

        categoryService.deleteCategory(entityId);
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T required(Map<String, Object> p, String key) {
        Object val = p.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required payload field: " + key);
        return (T) val;
    }

    private UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
