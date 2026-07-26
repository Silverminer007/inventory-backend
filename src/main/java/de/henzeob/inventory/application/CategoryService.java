package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.repository.CategoryRepository;
import de.henzeob.inventory.repository.ContainerRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class CategoryService {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    ContainerRepository containerRepository;

    @Inject
    ItemRepository itemRepository;

    public Category getExisting(UUID id) {
        return categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    public Category create(UUID id, String name, String description, String shortCode, Integer hue, LocalDateTime createdAt) {
        Category category = new Category();
        category.id = id;
        category.name = name;
        category.description = description;
        category.shortCode = shortCode;
        category.hue = hue;
        category.createdAt = createdAt;
        categoryRepository.persist(category);
        return category;
    }

    public void delete(UUID id) {
        Category category = getExisting(id);
        if (containerRepository.existsByCategory(id) || itemRepository.existsByCategory(id)) {
            throw new IllegalArgumentException("Category is still referenced by a container or item: " + id);
        }
        categoryRepository.delete(category);
    }
}
