package de.henzeob.inventory.mapper;

import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.model.entity.ItemTag;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Collectors;

@ApplicationScoped
public class ItemMapper {

    public ItemDTO toDTO(Item item) {
        if (item == null) return null;

        ItemDTO dto = new ItemDTO();
        dto.id = item.id;
        dto.name = item.name;
        dto.description = item.description;

        dto.containerId = item.getContainerId();
        dto.position = item.position;

        dto.quantity = item.quantity;
        dto.barcode = item.barcode;
        dto.qrCode = item.qrCode;
        dto.tags = item.tags.stream().map(ItemTag::getTag).collect(Collectors.toSet());

        dto.lastModified = item.lastModified;
        dto.version = item.version;
        dto.createdAt = item.createdAt;

        // Computed fields
        dto.locationPath = item.getLocationPath();
        dto.containerType = item.getContainerType();

        ItemDTO.CategoryInfo categoryInfo = new ItemDTO.CategoryInfo();
        categoryInfo.id = item.category.id;
        categoryInfo.name = item.category.name;
        categoryInfo.shortCode = item.category.shortCode;
        dto.category = categoryInfo;

        return dto;
    }

    public ItemDTO toDTOWithContainer(Item item) {
        ItemDTO dto = toDTO(item);

        if (dto != null && item.container != null) {
            ItemDTO.ContainerInfo container = new ItemDTO.ContainerInfo();
            container.type = item.container.containerType.name();
            container.id = item.container.id;
            container.name = item.container.name;
            container.path = item.container.getFullPath();
            dto.container = container;
        }

        return dto;
    }

    public void updateEntity(Item item, ItemDTO dto) {
        if (item == null || dto == null) return;

        item.name = dto.name;
        item.description = dto.description;
        item.position = dto.position;
        item.quantity = dto.quantity != null ? dto.quantity : 1;
        item.barcode = dto.barcode;
    }
}
