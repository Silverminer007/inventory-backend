package de.henzeob.inventory.application;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.repository.ImageRepository;
import de.henzeob.inventory.repository.ItemRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class ItemService {

    @Inject
    ItemRepository itemRepository;

    @Inject
    ImageRepository imageRepository;

    public Item getExisting(UUID id) {
        return itemRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    public Item create(UUID id, String name, String description, Container container, String position,
                        int quantity, LocalDateTime createdAt) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.description = description;
        item.container = container;
        item.position = position;
        item.quantity = quantity;
        item.createdAt = createdAt;
        itemRepository.persist(item);
        return item;
    }

    public void delete(UUID id) {
        Item item = getExisting(id);
        if (imageRepository.existsByItem(id)) {
            throw new IllegalArgumentException("Item is still referenced by an image: " + id);
        }
        itemRepository.delete(item);
    }
}
