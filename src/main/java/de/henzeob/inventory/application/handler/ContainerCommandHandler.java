package de.henzeob.inventory.application.handler;

import de.henzeob.inventory.application.ContainerService;
import de.henzeob.inventory.mapper.ContainerMapper;
import de.henzeob.inventory.model.dto.ContainerDTO;
import de.henzeob.inventory.model.entity.Command;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.ContainerType;
import de.henzeob.inventory.model.enums.CommandType;
import de.henzeob.inventory.repository.CommandRepository;
import de.henzeob.inventory.repository.ContainerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class ContainerCommandHandler {

    @Inject
    ContainerService containerService;

    @Inject
    ContainerMapper containerMapper;

    @Inject
    ContainerRepository containerRepository;

    @Inject
    CommandRepository commandRepository;

    public Object handle(CommandType type, Command command, String userId) {
        Map<String, Object> p = command.payload;
        return switch (type) {
            case CONTAINER_CREATE -> handleCreate(p, userId);
            case CONTAINER_UPDATE -> handleUpdate(command.entityId, p, userId);
            case CONTAINER_DELETE -> handleDelete(command.entityId, userId, p);
            case CONTAINER_MOVE   -> handleMove(command.entityId, p, userId);
            default -> throw new IllegalArgumentException("Not a CONTAINER command: " + type);
        };
    }

    private ContainerDTO handleCreate(Map<String, Object> p, String userId) {
        Container container = new Container();
        container.name = required(p, "name");
        container.description = (String) p.get("description");
        container.containerType = ContainerType.valueOf(required(p, "containerType").toString());
        container.position = (String) p.get("position");
        if (p.get("locationType") != null) {
            container.locationType = Container.LocationType.valueOf(p.get("locationType").toString());
        }
        container.location = (String) p.get("location");
        Long parentId = toLong(p.get("parentContainerId"));
        Container created = containerService.createContainer(container, parentId, userId);
        return containerMapper.toDTO(created);
    }

    private Object handleUpdate(Long entityId, Map<String, Object> p, String userId) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        Container container = containerRepository.findByIdAndUser(entityId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + entityId));

        if (force || clientVersion == null || container.version.equals(clientVersion)) {
            return applyUpdate(entityId, p, userId);
        }

        // Stale version: 3-way merge using server command history.
        long versionGap = container.version - clientVersion;
        Set<String> serverChanged = serverChangedContainerFields(entityId, versionGap);

        List<String> conflictingFields = new ArrayList<>();

        if (p.containsKey("name") && !Objects.equals(p.get("name"), container.name)
                && serverChanged.contains("name")) {
            conflictingFields.add("name");
        }
        if (p.containsKey("description") && !Objects.equals(p.get("description"), container.description)
                && serverChanged.contains("description")) {
            conflictingFields.add("description");
        }
        if (p.containsKey("position") && !Objects.equals(p.get("position"), container.position)
                && serverChanged.contains("position")) {
            conflictingFields.add("position");
        }
        if (p.containsKey("locationType")) {
            String serverLocationType = container.locationType != null ? container.locationType.name() : null;
            if (!Objects.equals(p.get("locationType"), serverLocationType)
                    && serverChanged.contains("locationType")) {
                conflictingFields.add("locationType");
            }
        }
        if (p.containsKey("location") && !Objects.equals(p.get("location"), container.location)
                && serverChanged.contains("location")) {
            conflictingFields.add("location");
        }

        if (!conflictingFields.isEmpty()) {
            ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
            info.clientVersion = clientVersion;
            info.serverVersion = container.version;
            info.conflictingFields = conflictingFields;
            info.serverSnapshot = containerMapper.toDTO(container);
            info.clientPayload = p;
            return new ConflictResult.Conflicted(info);
        }

        // Auto-merge: apply client's changes on top of current server state.
        ContainerDTO overlayDto = containerMapper.toDTO(container);
        if (p.containsKey("name"))         overlayDto.name = (String) p.get("name");
        if (p.containsKey("description"))  overlayDto.description = (String) p.get("description");
        if (p.containsKey("position"))     overlayDto.position = (String) p.get("position");
        if (p.containsKey("locationType")) overlayDto.locationType = (String) p.get("locationType");
        if (p.containsKey("location"))     overlayDto.location = (String) p.get("location");
        return containerService.updateContainer(entityId, overlayDto, userId);
    }

    /**
     * Returns the set of payload field names found in CONTAINER_UPDATE commands applied
     * within the last {@code versionGap} modifications for this entity.
     */
    private Set<String> serverChangedContainerFields(Long entityId, long versionGap) {
        if (versionGap <= 0) return Set.of();
        List<Command> recent = commandRepository.findRecentApplied(
                entityId, "CONTAINER", (int) Math.min(versionGap, 100));
        Set<String> meta = Set.of("version", "force", "containerType", "parentContainerId", "newParentContainerId");
        Set<String> changed = new HashSet<>();
        for (Command cmd : recent) {
            if (cmd.commandType == CommandType.CONTAINER_UPDATE && cmd.payload != null) {
                for (String key : cmd.payload.keySet()) {
                    if (!meta.contains(key)) changed.add(key);
                }
            }
        }
        return changed;
    }

    private ContainerDTO applyUpdate(Long entityId, Map<String, Object> p, String userId) {
        ContainerDTO dto = new ContainerDTO();
        dto.name = (String) p.get("name");
        dto.description = (String) p.get("description");
        dto.position = (String) p.get("position");
        dto.locationType = (String) p.get("locationType");
        dto.location = (String) p.get("location");
        dto.version = toLong(p.get("version"));
        return containerService.updateContainer(entityId, dto, userId);
    }

    private Object handleDelete(Long entityId, String userId, Map<String, Object> p) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        if (!force && clientVersion != null) {
            Container container = containerRepository.findByIdAndUser(entityId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Container not found: " + entityId));
            if (container.version > clientVersion) {
                ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
                info.clientVersion = clientVersion;
                info.serverVersion = container.version;
                info.conflictingFields = List.of();
                info.serverSnapshot = containerMapper.toDTO(container);
                info.clientPayload = p;
                return new ConflictResult.Conflicted(info);
            }
        }

        containerService.deleteContainer(entityId, userId);
        return null;
    }

    private Object handleMove(Long entityId, Map<String, Object> p, String userId) {
        Long clientVersion = toLong(p.get("version"));
        boolean force = Boolean.TRUE.equals(p.get("force"));

        if (!force && clientVersion != null) {
            Container container = containerRepository.findByIdAndUser(entityId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Container not found: " + entityId));
            if (container.version > clientVersion) {
                ConflictResult.ConflictInfo info = new ConflictResult.ConflictInfo();
                info.clientVersion = clientVersion;
                info.serverVersion = container.version;
                info.conflictingFields = List.of();
                info.serverSnapshot = containerMapper.toDTO(container);
                info.clientPayload = p;
                return new ConflictResult.Conflicted(info);
            }
        }

        Long newParentId = toLong(p.get("newParentContainerId"));
        Container moved = containerService.moveContainer(entityId, newParentId, userId);
        return containerMapper.toDTOWithChildren(moved);
    }

    @SuppressWarnings("unchecked")
    private <T> T required(Map<String, Object> p, String key) {
        Object val = p.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required payload field: " + key);
        return (T) val;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
