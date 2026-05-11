package de.henzeob.inventory.application;

import de.henzeob.inventory.mapper.CategoryMapper;
import de.henzeob.inventory.model.dto.CategoryDTO;
import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CategoryService {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CategoryMapper categoryMapper;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllSorted().stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> searchByName(String query) {
        return categoryRepository.searchByName(query).stream()
                .map(categoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO getCategoryByShortCode(String shortCode) {
        Category category = categoryRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Kategorie nicht gefunden"));
        return categoryMapper.toDTO(category);
    }

    public Category getCategoryEntity(UUID id) {
        return categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Kategorie nicht gefunden"));
    }

    public CategoryDTO getCategory(UUID id) {
        return categoryMapper.toDTO(getCategoryEntity(id));
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        Category category = new Category();
        if (dto.id != null) category.id = dto.id;
        categoryMapper.updateEntity(category, dto);
        categoryRepository.persist(category);
        return categoryMapper.toDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(UUID id, CategoryDTO dto) {
        Category category = categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Kategorie nicht gefunden"));
        categoryMapper.updateEntity(category, dto);
        categoryRepository.persist(category);
        return categoryMapper.toDTO(category);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Kategorie nicht gefunden"));
        categoryRepository.delete(category);
    }
}