package de.henzeob.inventory.mapper;

import de.henzeob.inventory.model.dto.CategoryDTO;
import de.henzeob.inventory.model.entity.Category;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Instant;

@ApplicationScoped
public class CategoryMapper {
    @Inject
    Clock clock;

    public CategoryDTO toDTO(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.id = entity.id;
        dto.name = entity.name;
        dto.description = entity.description;
        dto.shortCode = entity.shortCode;
        dto.version = entity.version;
        dto.createdAt = entity.createdAt;
        dto.lastModified = entity.lastModified;
        return dto;
    }

    public void updateEntity(Category entity, CategoryDTO dto) {
        if (entity == null || dto == null) return;

        entity.name = dto.name;
        entity.description = dto.description;
        entity.shortCode = dto.shortCode;
        entity.lastModified = Instant.now(clock);
    }
}
