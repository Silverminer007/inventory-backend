package de.henzeob.inventory.mapper;

import de.henzeob.inventory.model.dto.CategorySummaryDTO;
import de.henzeob.inventory.model.dto.ContainerDTO;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.ContainerType;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Collectors;

@ApplicationScoped
public class ContainerMapper {

    public ContainerDTO toDTO(Container container) {
        if (container == null) return null;

        ContainerDTO dto = new ContainerDTO();
        dto.id = container.id;
        dto.name = container.name;
        dto.description = container.description;
        dto.containerType = container.containerType.name();
        dto.location = container.location;
        dto.position = container.position;
        dto.qrCode = container.qrCode;
        dto.parentContainerId = container.parentContainer != null ? container.parentContainer.id : null;

        dto.lastModified = container.lastModified;
        dto.version = container.version;
        dto.createdAt = container.createdAt;

        dto.locationPath = container.getLocationPath();
        dto.itemCount = container.items.size();
        dto.totalItemCount = container.getTotalItemCount();

        if (container.parentContainer != null) {
            ContainerDTO.ParentInfo parent = new ContainerDTO.ParentInfo();
            parent.id = container.parentContainer.id;
            parent.name = container.parentContainer.name;
            parent.containerType = container.parentContainer.containerType.name();
            dto.parent = parent;
        }

        CategorySummaryDTO categoryInfo = new CategorySummaryDTO();
        categoryInfo.id = container.primaryCategory.id;
        categoryInfo.name = container.primaryCategory.name;
        categoryInfo.shortCode = container.primaryCategory.shortCode;
        dto.primaryCategory = categoryInfo;

        return dto;
    }

    public ContainerDTO toDTOWithChildren(Container container) {
        ContainerDTO dto = toDTO(container);
        if (dto != null && container.childContainers != null) {
            dto.children = container.childContainers.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }
        return dto;
    }

    public void updateEntity(Container container, ContainerDTO dto) {
        if (container == null || dto == null) return;

        container.name = dto.name;
        container.description = dto.description;
        container.position = dto.position;

        // Only update location fields for ROOMs
        if (container.containerType == ContainerType.ROOM) {
            container.location = dto.location;
        }
    }
}
