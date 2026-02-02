package de.henzeob.inventory.application;

import de.henzeob.inventory.mapper.ContainerMapper;
import de.henzeob.inventory.model.dto.ContainerDTO;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.ContainerType;
import de.henzeob.inventory.repository.ContainerRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContainerService {

    @Inject
    ContainerRepository containerRepository;

    @Inject
    ContainerMapper containerMapper;

    @Inject
    AuditLogService auditLogService;

    /**
     * Find container by ID for a user, or throw NotFoundException.
     */
    public Container getContainer(Long id, String userId) {
        return containerRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Container nicht gefunden"));
    }

    /**
     * Create a new container with parent validation.
     */
    @Transactional
    public Container createContainer(Container container, Long parentContainerId, String userId) {
        container.userId = userId;

        if (parentContainerId != null) {
            Container parent = getContainer(parentContainerId, userId);
            validateParentChild(container.containerType, parent);
            container.parentContainer = parent;
        } else if (container.containerType != ContainerType.ROOM) {
            throw new IllegalArgumentException("Nur Räume dürfen keinen übergeordneten Container haben");
        }

        containerRepository.persist(container);
        auditLogService.logCreate(userId, "CONTAINER", container.id, container.name, container);

        return container;
    }

    /**
     * Move a container to a new parent.
     */
    @Transactional
    public Container moveContainer(Long containerId, Long newParentId, String userId) {
        Container container = getContainer(containerId, userId);
        String oldLocation = container.getFullPath();

        if (container.containerType == ContainerType.ROOM) {
            throw new IllegalArgumentException("Räume können nicht verschoben werden");
        }

        Container newParent = getContainer(newParentId, userId);
        validateParentChild(container.containerType, newParent);
        validateNoCircularReference(containerId, newParentId, userId);

        container.parentContainer = newParent;
        containerRepository.persist(container);

        String newLocation = container.getFullPath();
        auditLogService.logMove(userId, "CONTAINER", container.id, container.name, oldLocation, newLocation);

        return container;
    }

    public List<ContainerDTO> getAllContainers(String userId) {
        return containerRepository.findByUser(userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ContainerDTO getContainerDTO(Long id, String userId) {
        Container container = getContainer(id, userId);
        return containerMapper.toDTOWithChildren(container);
    }

    public List<ContainerDTO> getRootContainers(String userId) {
        return containerRepository.findRootContainers(userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContainerDTO> getChildContainers(Long parentId, String userId) {
        getContainer(parentId, userId);
        return containerRepository.findByParentAndUser(parentId, userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContainerDTO> getContainersByType(String type, String userId) {
        ContainerType containerType = ContainerType.valueOf(type);
        return containerRepository.findByTypeAndUser(containerType, userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContainerDTO updateContainer(Long id, ContainerDTO dto, String userId) {
        Container container = getContainer(id, userId);

        Object oldValues = captureContainerState(container);

        containerMapper.updateEntity(container, dto);

        containerRepository.persist(container);

        auditLogService.logUpdate(userId, "CONTAINER", container.id, container.name, oldValues, container);

        return containerMapper.toDTO(container);
    }

    @Transactional
    public void deleteContainer(Long id, String userId) {
        Container container = getContainer(id, userId);

        if (!container.childContainers.isEmpty()) {
            throw new IllegalArgumentException("Container kann nicht gelöscht werden: enthält untergeordnete Container");
        }
        if (!container.items.isEmpty()) {
            throw new IllegalArgumentException("Container kann nicht gelöscht werden: enthält Gegenstände");
        }

        auditLogService.logDelete(userId, "CONTAINER", container.id, container.name, container);

        containerRepository.delete(container);
    }

    private Object captureContainerState(Container container) {
        return new Object() {
            public String name = container.name;
            public String description = container.description;
            public String position = container.position;
            public String location = container.location;
        };
    }

    private void validateParentChild(ContainerType childType, Container parent) {
        switch (childType) {
            case ROOM -> throw new IllegalArgumentException("Räume dürfen keinen übergeordneten Container haben");
            case SHELF -> {
                if (parent.containerType != ContainerType.ROOM) {
                    throw new IllegalArgumentException("Regale müssen in einem Raum sein");
                }
            }
            case BOX -> {
                // BOX can be in ROOM, SHELF, or another BOX
            }
        }
    }

    private void validateNoCircularReference(Long containerId, Long newParentId, String userId) {
        Long currentId = newParentId;
        int depth = 0;
        while (currentId != null && depth < 20) {
            if (currentId.equals(containerId)) {
                throw new IllegalArgumentException("Zirkuläre Referenz: Container kann nicht in sich selbst verschoben werden");
            }
            Container current = containerRepository.findByIdAndUser(currentId, userId).orElse(null);
            currentId = current != null && current.parentContainer != null ? current.parentContainer.id : null;
            depth++;
        }
    }
}
