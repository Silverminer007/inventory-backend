package de.henzeob.inventory.application;

import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.mapper.ItemMapper;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ItemService {

    @Inject
    ItemRepository itemRepository;

    @Inject
    ItemMapper itemMapper;

    @Inject
    TaggingService taggingService;

    @Inject
    AuditLogService auditLogService;

    @Inject
    ContainerService containerService;

    @Inject
    SynonymService synonymService;

    @Inject
    SynonymGenerationService synonymGenerationService;

    /**
     * Get all items for a user
     */
    public List<ItemDTO> getAllItems(String userId) {
        return itemRepository.findByUser(userId)
                .stream()
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get item by ID
     */
    public ItemDTO getItem(Long id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        return itemMapper.toDTOWithContainer(item);
    }

    /**
     * Create new item
     */
    @Transactional
    public ItemDTO createItem(ItemDTO dto, String userId) {
        Item item = new Item();
        item.userId = userId;

        itemMapper.updateEntity(item, dto);

        // Set location
        setLocation(item, dto.containerId, userId);

        // Auto-tagging
        if (dto.tags == null || dto.tags.isEmpty()) {
            Set<String> autoTags = taggingService.generateTags(dto.name, dto.description);
            item.tags.addAll(autoTags);
        }

        itemRepository.persist(item);

        // Audit log
        auditLogService.logCreate(userId, "ITEM", item.id, item.name, item);

        // Auto-generate synonyms
        synonymGenerationService.generateSynonyms(item.name, userId);

        return itemMapper.toDTO(item);
    }

    /**
     * Update item
     */
    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO dto, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        // Store old values for audit
        Object oldValues = captureItemState(item);

        itemMapper.updateEntity(item, dto);

        itemRepository.persist(item);

        // Audit log
        auditLogService.logUpdate(userId, "ITEM", item.id, item.name, oldValues, item);

        return itemMapper.toDTO(item);
    }

    /**
     * Move item to different container
     */
    @Transactional
    public ItemDTO moveItem(Long id, String userId, Long containerId) {
        Item item = itemRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        String oldLocation = item.getLocationPath();

        Container container = containerService.getContainer(containerId, userId);
        item.container = container;

        itemRepository.persist(item);

        // Audit log
        String newLocation = item.getLocationPath();
        auditLogService.logMove(userId, "ITEM", item.id, item.name, oldLocation, newLocation);

        return itemMapper.toDTO(item);
    }

    /**
     * Delete item
     */
    @Transactional
    public void deleteItem(Long id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId)
                .orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        // Audit log
        auditLogService.logDelete(userId, "ITEM", item.id, item.name, item);

        itemRepository.delete(item);
    }

    /**
     * Search items with synonym expansion
     */
    public List<ItemDTO> searchItems(String query, String userId) {
        Set<String> searchTerms = synonymService.expandSearchTerms(query, userId);

        List<Item> results = new ArrayList<>();

        for (String term : searchTerms) {
            // Exact / LIKE search
            List<Item> exactResults = itemRepository.searchByName(term, userId);
            exactResults.stream()
                    .filter(item -> results.stream().noneMatch(r -> r.id.equals(item.id)))
                    .forEach(results::add);

            // Fuzzy search if still few results
            if (results.size() < 5) {
                List<Item> fuzzyResults = itemRepository.fuzzySearch(term, userId, 0.3);
                fuzzyResults.stream()
                        .filter(item -> results.stream().noneMatch(r -> r.id.equals(item.id)))
                        .forEach(results::add);
            }
        }

        return results.stream()
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get items by tag
     */
    public List<ItemDTO> getItemsByTag(String tag, String userId) {
        return itemRepository.findByTag(tag, userId)
                .stream()
                .map(itemMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Helper methods
    private void setLocation(Item item, Long containerId, String userId) {
        if (containerId == null) {
            throw new IllegalArgumentException("Item muss einen Standort haben");
        }
        Container container = containerService.getContainer(containerId, userId);
        item.container = container;
    }

    private Object captureItemState(Item item) {
        // Für Audit Log - vereinfacht
        return new Object() {
            public String name = item.name;
            public String description = item.description;
            public Integer quantity = item.quantity;
            public String location = item.getLocationPath();
        };
    }
}
