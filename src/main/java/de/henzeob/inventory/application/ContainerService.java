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
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContainerService {

    @Inject
    ContainerRepository containerRepository;

    @Inject
    ContainerMapper containerMapper;

    @Inject
    CategoryService categoryService;

    public Container getContainer(UUID id, String userId) {
        return containerRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Container nicht gefunden"));
    }

    @Transactional
    public Container createContainer(Container container, UUID parentContainerId, UUID categoryId, String userId) {
        container.userId = userId;

        if (categoryId != null) {
            container.primaryCategory = categoryService.getCategoryEntity(categoryId);
        }

        if (parentContainerId != null) {
            Container parent = getContainer(parentContainerId, userId);
            validateParentChild(container.containerType, parent);
            container.parentContainer = parent;
        } else if (container.containerType != ContainerType.ROOM) {
            throw new IllegalArgumentException("Nur Räume dürfen keinen übergeordneten Container haben");
        }

        containerRepository.insert(container);

        return container;
    }

    @Transactional
    public Container moveContainer(UUID containerId, UUID newParentId, String userId) {
        Container container = getContainer(containerId, userId);

        if (container.containerType == ContainerType.ROOM) {
            throw new IllegalArgumentException("Räume können nicht verschoben werden");
        }

        Container newParent = getContainer(newParentId, userId);
        validateParentChild(container.containerType, newParent);
        validateNoCircularReference(containerId, newParentId, userId);

        container.parentContainer = newParent;
        containerRepository.persist(container);

        return container;
    }

    public List<ContainerDTO> getAllContainers(String userId) {
        return containerRepository.findByUser(userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContainerDTO> searchContainers(String query, ContainerType type, String userId) {
        return containerRepository.searchByNameOrDescription(query, type, userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ContainerDTO getContainerDTO(UUID id, String userId) {
        Container container = getContainer(id, userId);
        return containerMapper.toDTOWithChildren(container);
    }

    public List<ContainerDTO> getRootContainers(String userId) {
        return containerRepository.findRootContainers(userId)
                .stream()
                .map(containerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ContainerDTO> getChildContainers(UUID parentId, String userId) {
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
    public ContainerDTO updateContainer(UUID id, ContainerDTO dto, String userId) {
        Container container = getContainer(id, userId);

        containerMapper.updateEntity(container, dto);

        if (dto.primaryCategory != null && dto.primaryCategory.id != null) {
            container.primaryCategory = categoryService.getCategoryEntity(dto.primaryCategory.id);
        }

        containerRepository.persist(container);

        return containerMapper.toDTO(container);
    }

    @Transactional
    public void deleteContainer(UUID id, String userId) {
        Container container = getContainer(id, userId);

        if (!container.childContainers.isEmpty()) {
            throw new IllegalArgumentException("Container kann nicht gelöscht werden: enthält untergeordnete Container");
        }
        if (!container.items.isEmpty()) {
            throw new IllegalArgumentException("Container kann nicht gelöscht werden: enthält Gegenstände");
        }

        containerRepository.delete(container);
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

    private void validateNoCircularReference(UUID containerId, UUID newParentId, String userId) {
        UUID currentId = newParentId;
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
