package de.henzeob.inventory.mapper;

import de.henzeob.inventory.model.dto.ImageDTO;
import de.henzeob.inventory.model.entity.Image;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImageMapper {

    public ImageDTO toDTO(Image image) {
        if (image == null) return null;

        ImageDTO dto = new ImageDTO();
        dto.id = image.id;
        dto.itemId = image.item != null ? image.item.id : null;
        dto.containerId = image.container != null ? image.container.id : null;
        dto.filename = image.filename;
        dto.contentType = image.contentType;
        dto.fileSize = image.fileSize;
        dto.isPrimary = image.isPrimary;
        dto.uploadedAt = image.uploadedAt;
        dto.url = image.s3Url;
        return dto;
    }
}
