package de.henzeob.inventory.api;

import de.henzeob.inventory.model.entity.Item;
import de.henzeob.inventory.repository.ItemRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.FUTURE;
import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.ROOT_ID;
import static de.henzeob.inventory.support.SyncTestSupport.applyExpectStatus;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.payload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Item Tests" section of DOMAIN-RULES.md. */
@QuarkusTest
public class ItemCommandTest {

    @Inject
    ItemRepository itemRepository;

    private UUID createCategory(UUID head, String shortCode) {
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Cat " + shortCode, "shortcode", shortCode, "created_at", PAST));
        return id;
    }

    @Test
    public void create_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Hammer", "container", ROOT_ID.toString(), "quantity", 3,
                        "description", "desc", "position", "shelf 2", "created_at", PAST));

        Item item = itemRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Hammer", item.name);
        assertEquals(ROOT_ID, item.container.id);
        assertEquals(3, item.quantity);
        assertEquals("desc", item.description);
        assertEquals("shelf 2", item.position);
    }

    @Test
    public void create_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Temp Item", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_DELETE", payload("id", id.toString()));

        assertTrue(itemRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void create_thenUpdate_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Drill", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Drill 2"));

        assertEquals("Drill 2", itemRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void create_update_delete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Saw", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Saw 2"));
        applyOk(head, "ITEM_DELETE", payload("id", id.toString()));

        assertTrue(itemRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void updateName_thenUpdateNameAgain_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "AAA", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "BBB"));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "CCC"));

        assertEquals("CCC", itemRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void updateName_thenUpdateDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "AAA", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        head = applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Renamed"));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "description", "new desc"));

        Item item = itemRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Renamed", item.name);
        assertEquals("new desc", item.description);
    }

    @Test
    public void create_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("name", "No Id", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withoutName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withBlankName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "   ", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withNameTwoChars_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Ab", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withoutContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "No Container", "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withNonExistentContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Orphan Item", "container", UUID.randomUUID().toString(),
                        "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withNonExistentCategory_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Cat Fail Item", "container", ROOT_ID.toString(),
                        "quantity", 1, "category", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withExistingCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "ICAT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Categorized Item", "container", ROOT_ID.toString(),
                        "quantity", 1, "category", categoryId.toString(), "created_at", PAST));

        assertEquals(categoryId, itemRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void create_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", "not-a-uuid", "name", "Invalid Id", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withInvalidUuidContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Invalid Container", "container", "not-a-uuid",
                        "quantity", 1, "created_at", PAST), 400);
    }

    @Test
    public void create_withQuantityZero_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Zero Qty", "container", ROOT_ID.toString(),
                        "quantity", 0, "created_at", PAST), 400);
    }

    @Test
    public void create_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Future Item", "container", ROOT_ID.toString(),
                        "quantity", 1, "created_at", FUTURE), 400);
    }

    @Test
    public void create_withCreatedAtInPast_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Past Item", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        assertTrue(itemRepository.findByIdOptional(id).isPresent());
    }

    @Test
    public void update_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_UPDATE", payload("name", "No Id"), 400);
    }

    @Test
    public void update_withoutName_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Stable", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "description", "d"));

        assertEquals("Stable", itemRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void update_withoutQuantity_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Qty Keep", "container", ROOT_ID.toString(), "quantity", 5, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Qty Keep 2"));

        assertEquals(5, itemRepository.findByIdOptional(id).orElseThrow().quantity);
    }

    @Test
    public void update_withoutCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "UICT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Cat Item", "container", ROOT_ID.toString(), "quantity", 1,
                        "category", categoryId.toString(), "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Cat Item 2"));

        assertEquals(categoryId, itemRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void update_withoutDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Desc Keep", "container", ROOT_ID.toString(), "quantity", 1, "description", "orig", "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Desc Keep 2"));

        assertEquals("orig", itemRepository.findByIdOptional(id).orElseThrow().description);
    }

    @Test
    public void update_withoutPosition_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Pos Keep", "container", ROOT_ID.toString(), "quantity", 1, "position", "left", "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Pos Keep 2"));

        assertEquals("left", itemRepository.findByIdOptional(id).orElseThrow().position);
    }

    @Test
    public void update_withoutPrimaryImage_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Img Keep", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Img Keep 2"));

        assertNull(itemRepository.findByIdOptional(id).orElseThrow().primaryImage);
    }

    @Test
    public void update_onlyId_leavesSnapshotUnchanged() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Untouched", "container", ROOT_ID.toString(), "quantity", 2,
                        "description", "d", "position", "p", "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString()));

        Item item = itemRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Untouched", item.name);
        assertEquals(2, item.quantity);
        assertEquals("d", item.description);
        assertEquals("p", item.position);
        assertEquals(ROOT_ID, item.container.id);
    }

    @Test
    public void update_withBlankName_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "   "), 400);
    }

    @Test
    public void update_withNameTwoChars_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Ab"), 400);
    }

    @Test
    public void update_withoutContainer_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "name", "Valid 2"));

        assertEquals(ROOT_ID, itemRepository.findByIdOptional(id).orElseThrow().container.id);
    }

    @Test
    public void update_withNonExistentContainer_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "container", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void update_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", "not-a-uuid", "name", "X"), 400);
    }

    @Test
    public void update_withInvalidUuidContainer_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "container", "not-a-uuid"), 400);
    }

    @Test
    public void update_withNonExistentCategory_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "category", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void update_withExistingCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "UPIT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyOk(head, "ITEM_UPDATE", payload("id", id.toString(), "category", categoryId.toString()));

        assertEquals(categoryId, itemRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void update_withInvalidUuidCategory_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "category", "not-a-uuid"), 400);
    }

    @Test
    public void update_withQuantityZero_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "quantity", 0), 400);
    }

    @Test
    public void update_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "created_at", FUTURE), 400);
    }

    @Test
    public void update_withCreatedAtInPast_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "created_at", PAST), 400);
    }

    @Test
    public void update_withEmptyCreatedAt_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", payload("id", id.toString(), "name", "Valid", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        applyExpectStatus(head, "ITEM_UPDATE", payload("id", id.toString(), "created_at", ""), 400);
    }

    @Test
    public void delete_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_DELETE", payload(), 400);
    }

    @Test
    public void delete_withNonExistentId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_DELETE", payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void create_withOnlyRequiredFields_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "ITEM_CREATE",
                payload("id", id.toString(), "name", "Minimal", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));

        Item item = itemRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Minimal", item.name);
        assertNull(item.description);
        assertNull(item.category);
        assertNull(item.position);
    }
}
