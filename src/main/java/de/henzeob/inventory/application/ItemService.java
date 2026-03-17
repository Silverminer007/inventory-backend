package de.henzeob.inventory.application;

import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.mapper.ItemMapper;
import de.henzeob.inventory.model.entity.ItemTag;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    ContainerService containerService;

    @Inject
    SynonymService synonymService;

    @Inject
    SynonymGenerationService synonymGenerationService;

    @Inject
    EntityManager entityManager;

    /**
     * Get all items for a user
     */
    public List<ItemDTO> getAllItems(String userId) {
        return itemRepository.findByUser(userId).stream().map(itemMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Get item by ID
     */
    public ItemDTO getItem(Long id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

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

        itemRepository.persist(item);

        // Auto-tagging
        if (dto.tags == null || dto.tags.isEmpty()) {
            Set<ItemTag> autoTags = taggingService.generateTags(dto.name, dto.description);
            for (ItemTag tag : autoTags) {
                tag.setItem(item);
                tag.persist();
            }
            item.tags.addAll(autoTags);
        } else {
            for (String tagStr : dto.tags) {
                ItemTag tag = new ItemTag();
                tag.setTag(tagStr);
                tag.setItem(item);
                tag.setTagType(ItemTag.TagType.USER);
                tag.persist();
                item.tags.add(tag);
            }
        }

        // Auto-generate synonyms
        synonymGenerationService.generateSynonyms(item.name, userId);

        return itemMapper.toDTO(item);
    }

    /**
     * Update item
     */
    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO dto, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        itemMapper.updateEntity(item, dto);

        List<ItemTag> oldItemTags = ItemTag.find("item.id = ?1", dto.id).list();

        List<String> newTags = dto.tags.stream().filter(tag -> oldItemTags.stream().noneMatch(t -> t.getTag().equals(tag))).toList();

        List<ItemTag> deletedItemTags = oldItemTags.stream().filter(t -> !dto.tags.contains(t.getTag())).toList();

        for (String tag : newTags) {
            ItemTag newTag = new ItemTag();
            newTag.setTag(tag);
            newTag.setItem(item);
            newTag.setTagType(ItemTag.TagType.USER);
            newTag.persist();
        }

        for (ItemTag tag : deletedItemTags) {
            tag.delete();
        }

        itemRepository.persist(item);

        return itemMapper.toDTO(item);
    }

    /**
     * Move item to different container
     */
    @Transactional
    public ItemDTO moveItem(Long id, String userId, Long containerId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        Container container = containerService.getContainer(containerId, userId);
        item.container = container;

        itemRepository.persist(item);

        return itemMapper.toDTO(item);
    }

    /**
     * Delete item
     */
    @Transactional
    public void deleteItem(Long id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        itemRepository.delete(item);
    }

    /**
     * Search items via Elasticsearch
     */
    public List<ItemDTO> searchItems(String query, List<String> tags, String userId) {
        SearchSession searchSession = Search.session(entityManager);

        List<Item> hits = searchSession.search(Item.class)
                .where(f -> {
                    var bool = f.bool();

                    // Always scope to the current user
                    bool.must(f.match().field("userId").matching(userId));

                    // Full-text search across all relevant fields
                    if (query != null && !query.isBlank()) {
                        bool.must(f.bool()
                                .should(f.match()
                                        .field("name").boost(3.0f)
                                        .field("description")
                                        .field("tags.tag").boost(2.0f)
                                        .field("container.name").boost(1.5f)
                                        .field("container.description")
                                        .matching(query))
                                .should(f.match()
                                        .fields("name", "description", "tags.tag")
                                        .matching(query)
                                        .fuzzy(1))
                        );
                    }

                    // Tag intersection: at least one supplied tag must be present on the item
                    if (tags != null && !tags.isEmpty()) {
                        var tagBool = f.bool();
                        for (String tag : tags) {
                            tagBool.should(f.match().field("tags.tag").matching(tag));
                        }
                        bool.must(tagBool);
                    }

                    return bool;
                })
                .sort(TypedSearchSortFactory::score)
                .fetchAllHits();

        return hits.stream().map(itemMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Get items by tag
     */
    public List<ItemDTO> getItemsByTag(String tag, String userId) {
        return itemRepository.findByTag(tag, userId).stream().map(itemMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all distinct tags for a user, optionally filtered by prefix
     */
    public List<String> getDistinctTags(String userId, String prefix) {
        List<String> tags = itemRepository.findDistinctTagsByUser(userId);
        if (prefix != null && !prefix.isBlank()) {
            String lowerPrefix = prefix.toLowerCase();
            tags = tags.stream().filter(tag -> tag.toLowerCase().startsWith(lowerPrefix)).collect(Collectors.toList());
        }
        return tags;
    }

    // Helper methods
    private void setLocation(Item item, Long containerId, String userId) {
        if (containerId == null) {
            throw new IllegalArgumentException("Item muss einen Standort haben");
        }
        Container container = containerService.getContainer(containerId, userId);
        item.container = container;
    }

}
