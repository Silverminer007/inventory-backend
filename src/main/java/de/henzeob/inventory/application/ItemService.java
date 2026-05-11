package de.henzeob.inventory.application;

import de.henzeob.inventory.model.dto.ItemDTO;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.mapper.ItemMapper;
import de.henzeob.inventory.model.entity.ItemTag;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ItemService {

    @Inject
    ItemRepository itemRepository;

    @Inject
    ItemMapper itemMapper;

    @Inject
    ContainerService containerService;

    @Inject
    CategoryService categoryService;

    public List<ItemDTO> getAllItems(String userId) {
        return itemRepository.findByUser(userId).stream().map(itemMapper::toDTO).collect(Collectors.toList());
    }

    public ItemDTO getItem(UUID id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));
        return itemMapper.toDTOWithContainer(item);
    }

    @Transactional
    public ItemDTO createItem(ItemDTO dto, String userId) {
        Item item = new Item();
        item.userId = userId;
        if (dto.id != null) {
            item.id = dto.id;
        }

        itemMapper.updateEntity(item, dto);
        setLocation(item, dto.containerId, userId);
        if (dto.category != null && dto.category.id != null) {
            item.category = categoryService.getCategoryEntity(dto.category.id);
        }
        itemRepository.insert(item);

        if (dto.tags != null && !dto.tags.isEmpty()) {
            for (String tagStr : dto.tags) {
                ItemTag tag = new ItemTag();
                tag.setTag(tagStr);
                tag.setItem(item);
                tag.setTagType(ItemTag.TagType.USER);
                tag.persist();
                item.tags.add(tag);
            }
        }

        return itemMapper.toDTO(item);
    }

    @Transactional
    public ItemDTO updateItem(UUID id, ItemDTO dto, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        itemMapper.updateEntity(item, dto);
        if (dto.category != null && dto.category.id != null) {
            item.category = categoryService.getCategoryEntity(dto.category.id);
        }
        item.lastModified = LocalDateTime.now();

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

    @Transactional
    public ItemDTO moveItem(UUID id, String userId, UUID containerId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));

        item.container = containerService.getContainer(containerId, userId);

        itemRepository.persist(item);

        return itemMapper.toDTO(item);
    }

    @Transactional
    public void deleteItem(UUID id, String userId) {
        Item item = itemRepository.findByIdAndUser(id, userId).orElseThrow(() -> new NotFoundException("Item nicht gefunden"));
        itemRepository.delete(item);
    }

    public List<ItemDTO> getItemsByTag(String tag, String userId) {
        return itemRepository.findByTag(tag, userId).stream().map(itemMapper::toDTO).collect(Collectors.toList());
    }

    public List<String> getDistinctTags(String userId, String prefix) {
        List<String> tags = itemRepository.findDistinctTagsByUser(userId);
        if (prefix != null && !prefix.isBlank()) {
            String lowerPrefix = prefix.toLowerCase();
            tags = tags.stream().filter(tag -> tag.toLowerCase().startsWith(lowerPrefix)).collect(Collectors.toList());
        }
        return tags;
    }

    private void setLocation(Item item, UUID containerId, String userId) {
        if (containerId == null) {
            throw new IllegalArgumentException("Item muss einen Standort haben");
        }
        item.container = containerService.getContainer(containerId, userId);
    }
}
