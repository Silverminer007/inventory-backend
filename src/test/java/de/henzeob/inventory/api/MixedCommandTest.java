package de.henzeob.inventory.api;

import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.repository.ContainerRepository;
import de.henzeob.inventory.repository.ItemRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.ROOT_ID;
import static de.henzeob.inventory.support.SyncTestSupport.applyExpectStatus;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.payload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Mixed Tests" section of DOMAIN-RULES.md. */
@QuarkusTest
public class MixedCommandTest {

    @Inject
    ContainerRepository containerRepository;

    @Inject
    ItemRepository itemRepository;

    private UUID category(UUID head, String shortCode) {
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Cat " + shortCode, "shortcode", shortCode, "created_at", PAST));
        return id;
    }

    private UUID container(UUID head, UUID parent) {
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Container " + id, "parent", parent.toString(), "created_at", PAST));
        return id;
    }

    private UUID item(UUID head, UUID container) {
        UUID id = UUID.randomUUID();
        applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Item " + id, "container", container.toString(), "quantity", 1, "created_at", PAST));
        return id;
    }

    // 1
    @Test
    public void categoryReferencedByItem_cannotBeDeleted() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX01");
        head = currentHead();
        UUID itemId = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", itemId.toString(), "name", "Ref Item", "container", ROOT_ID.toString(),
                "quantity", 1, "category", categoryId.toString(), "created_at", PAST));

        applyExpectStatus(head, "CATEGORY_DELETE", payload("id", categoryId.toString()), 400);
    }

    // 2
    @Test
    public void categoryReferencedByItem_canStillBeUpdated() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX02");
        head = currentHead();
        UUID itemId = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", itemId.toString(), "name", "Ref Item", "container", ROOT_ID.toString(),
                "quantity", 1, "category", categoryId.toString(), "created_at", PAST));

        applyOk(head, "CATEGORY_UPDATE", payload("id", categoryId.toString(), "name", "Renamed Category"));
    }

    // 3
    @Test
    public void categoryReferencedViaItemUpdate_cannotBeDeleted() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX03");
        head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "category", categoryId.toString()));

        applyExpectStatus(head, "CATEGORY_DELETE", payload("id", categoryId.toString()), 400);
    }

    // 4
    @Test
    public void itemCreateWithCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX04");
        head = currentHead();
        applyOk(head, "ITEM_CREATE", payload("id", UUID.randomUUID().toString(), "name", "Cat Item", "container", ROOT_ID.toString(),
                "quantity", 1, "category", categoryId.toString(), "created_at", PAST));
    }

    // 5
    @Test
    public void itemUpdateWithCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX05");
        head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "category", categoryId.toString()));
    }

    // 6
    @Test
    public void categoryReferencedByContainer_cannotBeDeleted() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX06");
        head = currentHead();
        UUID containerId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", containerId.toString(), "name", "Ref Container",
                "parent", ROOT_ID.toString(), "category", categoryId.toString(), "created_at", PAST));

        applyExpectStatus(head, "CATEGORY_DELETE", payload("id", categoryId.toString()), 400);
    }

    // 7
    @Test
    public void categoryReferencedByContainer_canStillBeUpdated() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX07");
        head = currentHead();
        UUID containerId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", containerId.toString(), "name", "Ref Container",
                "parent", ROOT_ID.toString(), "category", categoryId.toString(), "created_at", PAST));

        applyOk(head, "CATEGORY_UPDATE", payload("id", categoryId.toString(), "name", "Renamed Category"));
    }

    // 8
    @Test
    public void categoryReferencedViaContainerUpdate_cannotBeDeleted() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX08");
        head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "category", categoryId.toString()));

        applyExpectStatus(head, "CATEGORY_DELETE", payload("id", categoryId.toString()), 400);
    }

    // 9
    @Test
    public void containerCreateWithCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX09");
        head = currentHead();
        applyOk(head, "CONTAINER_CREATE", payload("id", UUID.randomUUID().toString(), "name", "Cat Container",
                "parent", ROOT_ID.toString(), "category", categoryId.toString(), "created_at", PAST));
    }

    // 10
    @Test
    public void containerUpdateWithCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = category(head, "MX10");
        head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "category", categoryId.toString()));
    }

    // 11
    @Test
    public void containerReferencedByItem_cannotBeDeleted() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        item(head, containerId);
        head = currentHead();

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", containerId.toString()), 400);
    }

    // 12
    @Test
    public void containerReferencedViaItemUpdate_cannotBeDeleted() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "container", containerId.toString()));

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", containerId.toString()), 400);
    }

    // 13
    @Test
    public void itemCreateWithContainer_succeeds() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        item(head, containerId);
    }

    // 14
    @Test
    public void itemUpdateWithContainer_succeeds() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "container", containerId.toString()));
    }

    // 15
    @Test
    public void containerReferencedAsParent_cannotBeDeleted() {
        UUID head = currentHead();
        UUID parentId = container(head, ROOT_ID);
        head = currentHead();
        container(head, parentId);
        head = currentHead();

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", parentId.toString()), 400);
    }

    // 16
    @Test
    public void containerReferencedAsParentViaUpdate_cannotBeDeleted() {
        UUID head = currentHead();
        UUID parentId = container(head, ROOT_ID);
        head = currentHead();
        UUID childId = container(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", childId.toString(), "parent", parentId.toString()));

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", parentId.toString()), 400);
    }

    // 17
    @Test
    public void containerCreateWithParent_succeeds() {
        UUID head = currentHead();
        UUID parentId = container(head, ROOT_ID);
        head = currentHead();
        container(head, parentId);
    }

    // 18
    @Test
    public void containerUpdateWithParent_succeeds() {
        UUID head = currentHead();
        UUID parentId = container(head, ROOT_ID);
        head = currentHead();
        UUID childId = container(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "CONTAINER_UPDATE", payload("id", childId.toString(), "parent", parentId.toString()));
    }

    // 19
    @Test
    public void itemWithImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", UUID.randomUUID().toString(), "item", itemId.toString(), "created_at", PAST));

        applyExpectStatus(head, "ITEM_DELETE", payload("id", itemId.toString()), 400);
    }

    // 20
    @Test
    public void itemImage_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", itemId.toString(), "created_at", PAST));

        applyOk(head, "ITEM_IMAGE_DELETE", payload("id", imageId.toString()));
    }

    // 21
    @Test
    public void itemWithPrimaryImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", itemId.toString(), "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", imageId.toString()));

        applyExpectStatus(head, "ITEM_DELETE", payload("id", itemId.toString()), 400);
    }

    // 22
    @Test
    public void primaryImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", itemId.toString(), "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", imageId.toString()));

        applyExpectStatus(head, "ITEM_IMAGE_DELETE", payload("id", imageId.toString()), 400);
    }

    // 23 + 24
    @Test
    public void clearingPrimaryImage_allowsImageThenItemDeletion() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", itemId.toString(), "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", imageId.toString()));

        java.util.Map<String, Object> clearPrimary = new java.util.LinkedHashMap<>();
        clearPrimary.put("id", itemId.toString());
        clearPrimary.put("primary_image", null);
        head = applyOk(head, "ITEM_UPDATE", clearPrimary);

        head = applyOk(head, "ITEM_IMAGE_DELETE", payload("id", imageId.toString()));
        applyOk(head, "ITEM_DELETE", payload("id", itemId.toString()));

        assertTrue(itemRepository.findByIdOptional(itemId).isEmpty());
    }

    // 25
    @Test
    public void itemImageCreate_succeeds() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "ITEM_IMAGE_CREATE", payload("id", UUID.randomUUID().toString(), "item", itemId.toString(), "created_at", PAST));
    }

    // 26
    @Test
    public void containerWithImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", UUID.randomUUID().toString(), "container", containerId.toString(), "created_at", PAST));

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", containerId.toString()), 400);
    }

    // 27
    @Test
    public void containerImage_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", imageId.toString(), "container", containerId.toString(), "created_at", PAST));

        applyOk(head, "CONTAINER_IMAGE_DELETE", payload("id", imageId.toString()));
    }

    // 28
    @Test
    public void containerWithPrimaryImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", imageId.toString(), "container", containerId.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "primary_image", imageId.toString()));

        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", containerId.toString()), 400);
    }

    // 29
    @Test
    public void containerPrimaryImage_cannotBeDeleted() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", imageId.toString(), "container", containerId.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "primary_image", imageId.toString()));

        applyExpectStatus(head, "CONTAINER_IMAGE_DELETE", payload("id", imageId.toString()), 400);
    }

    // 30 + 31
    @Test
    public void clearingContainerPrimaryImage_allowsImageThenContainerDeletion() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", imageId.toString(), "container", containerId.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "primary_image", imageId.toString()));

        java.util.Map<String, Object> clearPrimary = new java.util.LinkedHashMap<>();
        clearPrimary.put("id", containerId.toString());
        clearPrimary.put("primary_image", null);
        head = applyOk(head, "CONTAINER_UPDATE", clearPrimary);

        head = applyOk(head, "CONTAINER_IMAGE_DELETE", payload("id", imageId.toString()));
        applyOk(head, "CONTAINER_DELETE", payload("id", containerId.toString()));

        assertTrue(containerRepository.findByIdOptional(containerId).isEmpty());
    }

    // 32
    @Test
    public void containerImageCreate_succeeds() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", UUID.randomUUID().toString(), "container", containerId.toString(), "created_at", PAST));
    }

    // 33
    @Test
    public void itemCreateWithNonExistentCategory_fails() {
        UUID head = currentHead();
        category(head, "MX33");
        head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Bad Cat Item", "container", ROOT_ID.toString(),
                        "quantity", 1, "category", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    // 34
    @Test
    public void containerCreateWithNonExistentCategory_fails() {
        UUID head = currentHead();
        category(head, "MX34");
        head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Bad Cat Container", "parent", ROOT_ID.toString(),
                        "category", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    // 35
    @Test
    public void primaryImageAcrossDifferentItems_fails() {
        UUID head = currentHead();
        UUID firstItemId = item(head, ROOT_ID);
        head = currentHead();
        UUID secondItemId = item(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", firstItemId.toString(), "created_at", PAST));

        applyExpectStatus(head, "ITEM_UPDATE", payload("id", secondItemId.toString(), "primary_image", imageId.toString()), 400);
    }

    // 36
    @Test
    public void itemUpdatePrimaryImage_nonExistent_fails() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", UUID.randomUUID().toString()), 400);
    }

    // 37
    @Test
    public void itemUpdatePrimaryImage_invalidUuid_fails() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", "not-a-uuid"), 400);
    }

    // 38
    @Test
    public void itemPrimaryImage_canBeSwitchedToSecondImage() {
        UUID head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID firstImageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", firstImageId.toString(), "item", itemId.toString(), "created_at", PAST));
        UUID secondImageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", secondImageId.toString(), "item", itemId.toString(), "created_at", PAST));

        applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", secondImageId.toString()));

        Item item = itemRepository.findByIdOptional(itemId).orElseThrow();
        assertEquals(secondImageId, item.primaryImage.id);
    }

    // 39
    @Test
    public void itemUpdatePrimaryImage_foreignItemImage_fails() {
        UUID head = currentHead();
        UUID otherItemId = item(head, ROOT_ID);
        head = currentHead();
        UUID itemId = item(head, ROOT_ID);
        head = currentHead();
        UUID foreignImageId = UUID.randomUUID();
        head = applyOk(head, "ITEM_IMAGE_CREATE", payload("id", foreignImageId.toString(), "item", otherItemId.toString(), "created_at", PAST));

        applyExpectStatus(head, "ITEM_UPDATE", payload("id", itemId.toString(), "primary_image", foreignImageId.toString()), 400);
    }

    // 40
    @Test
    public void primaryImageAcrossDifferentContainers_fails() {
        UUID head = currentHead();
        UUID firstContainerId = container(head, ROOT_ID);
        head = currentHead();
        UUID secondContainerId = container(head, ROOT_ID);
        head = currentHead();
        UUID imageId = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", imageId.toString(), "container", firstContainerId.toString(), "created_at", PAST));

        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", secondContainerId.toString(), "primary_image", imageId.toString()), 400);
    }

    // 41
    @Test
    public void containerUpdatePrimaryImage_nonExistent_fails() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "primary_image", UUID.randomUUID().toString()), 400);
    }

    // 42
    @Test
    public void containerUpdatePrimaryImage_invalidUuid_fails() {
        UUID head = currentHead();
        UUID containerId = container(head, ROOT_ID);
        head = currentHead();
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", containerId.toString(), "primary_image", "not-a-uuid"), 400);
    }
}
