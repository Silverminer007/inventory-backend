package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.ItemService;
import de.henzeob.inventory.mapper.ItemMapper;
import de.henzeob.inventory.model.dto.CategorySummaryDTO;
import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.CommandRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static de.henzeob.inventory.application.handler.CommandPayloadUtils.required;
import static de.henzeob.inventory.application.handler.CommandPayloadUtils.toInteger;
import static de.henzeob.inventory.application.handler.CommandPayloadUtils.toLong;
import static de.henzeob.inventory.application.handler.CommandPayloadUtils.toUUID;

@ApplicationScoped
public class ItemCommandHandler {

    @Inject
    ItemService itemService;

    @Inject
    ItemRepository itemRepository;

    @Inject
    ItemMapper itemMapper;

    @Inject
    CommandRepository commandRepository;

    public Object handle(CommandType type, Command command, String userId) {
        Map<String, Object> p = command.payload;
        return switch (type) {
            case ITEM_CREATE -> handleCreate(p, userId);
            case ITEM_UPDATE -> handleUpdate(command.entityId, p, userId);
            case ITEM_DELETE -> handleDelete(command.entityId, userId, p);
            case ITEM_MOVE   -> handleMove(command.entityId, p, userId);
            default -> throw new IllegalArgumentException("Not an ITEM command: " + type);
        };
    }

    private ItemDTO handleCreate(Map<String, Object> p, String userId) {
        ItemDTO dto = new ItemDTO();
        dto.id = toUUID(p.get("id")); // optional client-provided UUID
        dto.name = required(p, "name");
        dto.description = (String) p.get("description");
        dto.containerId = toUUID(p.get("containerId"));
        dto.position = (String) p.get("position");
        dto.quantity = p.get("quantity") != null ? toInteger(p.get("quantity")) : 1;
        dto.barcode = (String) p.get("barcode");
        if (p.get("tags") instanceof List<?> rawTags) {
            Set<String> tags = new LinkedHashSet<>();
            for (Object t : rawTags) tags.add(t.toString());
            dto.tags = tags;
        }
        if (p.get("category") instanceof Map<?, ?> catMap) {
            dto.category = new CategorySummaryDTO();
            dto.category.id = toUUID(catMap.get("id"));
        }
        return itemService.createItem(dto, userId);
    }

    private Object handleUpdate(UUID entityId, Map<String, Object> p, String userId) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        Item item = itemRepository.findByIdAndUser(entityId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + entityId));

        if (force || clientVersion == null || item.version.equals(clientVersion)) {
            return applyUpdate(entityId, p, userId);
        }

        long versionGap = item.version - clientVersion;
        Set<String> serverChanged = serverChangedItemFields(entityId, versionGap);

        List<String> conflictingFields = new ArrayList<>();

        if (p.containsKey("name") && !Objects.equals(p.get("name"), item.name)
                && serverChanged.contains("name")) {
            conflictingFields.add("name");
        }
        if (p.containsKey("description") && !Objects.equals(p.get("description"), item.description)
                && serverChanged.contains("description")) {
            conflictingFields.add("description");
        }
        if (p.containsKey("position") && !Objects.equals(p.get("position"), item.position)
                && serverChanged.contains("position")) {
            conflictingFields.add("position");
        }
        if (p.containsKey("quantity") && p.get("quantity") != null) {
            Integer clientQty = toInteger(p.get("quantity"));
            if (!Objects.equals(clientQty, item.quantity) && serverChanged.contains("quantity")) {
                conflictingFields.add("quantity");
            }
        }
        if (p.containsKey("barcode") && !Objects.equals(p.get("barcode"), item.barcode)
                && serverChanged.contains("barcode")) {
            conflictingFields.add("barcode");
        }
        if (p.containsKey("tags")) {
            Set<String> clientTags = extractTags(p);
            Set<String> serverTags = itemMapper.toDTO(item).tags;
            if (!Objects.equals(clientTags, serverTags)) {
                conflictingFields.add("tags");
            }
        }
        if (p.containsKey("category") && p.get("category") instanceof Map<?, ?> catMap) {
            UUID clientCategoryId = toUUID(catMap.get("id"));
            UUID serverCategoryId = item.category != null ? item.category.id : null;
            if (!Objects.equals(clientCategoryId, serverCategoryId) && serverChanged.contains("category")) {
                conflictingFields.add("category");
            }
        }

        if (!conflictingFields.isEmpty()) {
            ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
            info.clientVersion = clientVersion;
            info.serverVersion = item.version;
            info.conflictingFields = conflictingFields;
            info.serverSnapshot = itemMapper.toDTO(item);
            info.clientPayload = p;
            return new ConflictResult.Conflicted(info);
        }

        // Auto-merge
        ItemDTO overlayDto = itemMapper.toDTO(item);
        if (p.containsKey("name"))        overlayDto.name = (String) p.get("name");
        if (p.containsKey("description")) overlayDto.description = (String) p.get("description");
        if (p.containsKey("position"))    overlayDto.position = (String) p.get("position");
        if (p.containsKey("quantity"))    overlayDto.quantity = p.get("quantity") != null ? toInteger(p.get("quantity")) : null;
        if (p.containsKey("barcode"))     overlayDto.barcode = (String) p.get("barcode");
        if (p.containsKey("tags"))        overlayDto.tags = extractTags(p);
        if (p.containsKey("category") && p.get("category") instanceof Map<?, ?> catMap) {
            overlayDto.category = new CategorySummaryDTO();
            overlayDto.category.id = toUUID(catMap.get("id"));
        }
        return itemService.updateItem(entityId, overlayDto, userId);
    }

    private Set<String> serverChangedItemFields(UUID entityId, long versionGap) {
        if (versionGap <= 0) return Set.of();
        List<Command> recent = commandRepository.findRecentApplied(
                entityId, "ITEM", (int) Math.min(versionGap, 100));
        Set<String> meta = Set.of("version", "force", "containerId");
        Set<String> changed = new HashSet<>();
        for (Command cmd : recent) {
            if (cmd.commandType == CommandType.ITEM_UPDATE && cmd.payload != null) {
                for (String key : cmd.payload.keySet()) {
                    if (!meta.contains(key)) changed.add(key);
                }
            }
        }
        return changed;
    }

    private ItemDTO applyUpdate(UUID entityId, Map<String, Object> p, String userId) {
        ItemDTO dto = new ItemDTO();
        dto.id = entityId;
        dto.name = (String) p.get("name");
        dto.description = (String) p.get("description");
        dto.position = (String) p.get("position");
        dto.quantity = p.get("quantity") != null ? toInteger(p.get("quantity")) : null;
        dto.barcode = (String) p.get("barcode");
        dto.version = toLong(p.get("version"));
        Set<String> tags = new LinkedHashSet<>();
        if (p.get("tags") instanceof List<?> rawTags) {
            for (Object t : rawTags) tags.add(t.toString());
        }
        dto.tags = tags;
        if (p.get("category") instanceof Map<?, ?> catMap) {
            dto.category = new CategorySummaryDTO();
            dto.category.id = toUUID(catMap.get("id"));
        }
        return itemService.updateItem(entityId, dto, userId);
    }

    private Object handleDelete(UUID entityId, String userId, Map<String, Object> p) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        if (!force && clientVersion != null) {
            Item item = itemRepository.findByIdAndUser(entityId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + entityId));
            if (item.version > clientVersion) {
                ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
                info.clientVersion = clientVersion;
                info.serverVersion = item.version;
                info.conflictingFields = List.of();
                info.serverSnapshot = itemMapper.toDTO(item);
                info.clientPayload = p;
                return new ConflictResult.Conflicted(info);
            }
        }

        itemService.deleteItem(entityId, userId);
        return null;
    }

    private Object handleMove(UUID entityId, Map<String, Object> p, String userId) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        if (!force && clientVersion != null) {
            Item item = itemRepository.findByIdAndUser(entityId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + entityId));
            if (item.version > clientVersion) {
                ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
                info.clientVersion = clientVersion;
                info.serverVersion = item.version;
                info.conflictingFields = List.of();
                info.serverSnapshot = itemMapper.toDTO(item);
                info.clientPayload = p;
                return new ConflictResult.Conflicted(info);
            }
        }

        UUID containerId = toUUID(required(p, "containerId"));
        return itemService.moveItem(entityId, userId, containerId);
    }

    private Set<String> extractTags(Map<String, Object> p) {
        Set<String> tags = new LinkedHashSet<>();
        if (p.get("tags") instanceof List<?> rawTags) {
            for (Object t : rawTags) tags.add(t.toString());
        }
        return tags;
    }
}
