package de.henzeob.inventory.api;

import de.henzeob.inventory.model.entity.Container;
import de.henzeob.inventory.repository.ContainerRepository;
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

/** "Container Tests" section of DOMAIN-RULES.md. */
@QuarkusTest
public class ContainerCommandTest {

    @Inject
    ContainerRepository containerRepository;

    private UUID createCategory(UUID head, String shortCode) {
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Cat " + shortCode, "shortcode", shortCode, "created_at", PAST));
        return id;
    }

    @Test
    public void create_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Garage", "parent", ROOT_ID.toString(),
                        "description", "desc", "position", "corner", "type", "ROOM", "created_at", PAST));

        Container container = containerRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Garage", container.name);
        assertEquals(ROOT_ID, container.parentContainer.id);
        assertEquals("desc", container.description);
        assertEquals("corner", container.position);
        assertEquals("ROOM", container.type);
    }

    @Test
    public void create_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Temp", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_DELETE", payload("id", id.toString()));

        assertTrue(containerRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void create_thenUpdate_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Shed", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Shed 2"));

        assertEquals("Shed 2", containerRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void create_update_delete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Barn", "parent", ROOT_ID.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Barn 2"));
        applyOk(head, "CONTAINER_DELETE", payload("id", id.toString()));

        assertTrue(containerRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void updateName_thenUpdateNameAgain_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "AAA", "parent", ROOT_ID.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "BBB"));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "CCC"));

        assertEquals("CCC", containerRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void updateName_thenUpdateDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "AAA", "parent", ROOT_ID.toString(), "created_at", PAST));
        head = applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Renamed"));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "description", "new desc"));

        Container container = containerRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Renamed", container.name);
        assertEquals("new desc", container.description);
    }

    @Test
    public void create_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("name", "No Id", "parent", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withoutName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "parent", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withBlankName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "   ", "parent", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withNameTwoChars_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Ab", "parent", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withoutParent_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "No Parent", "created_at", PAST), 400);
    }

    @Test
    public void create_withNonExistentParent_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Orphan", "parent", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withNonExistentCategory_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Cat Fail", "parent", ROOT_ID.toString(),
                        "category", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withExistingCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "CCAT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Categorized", "parent", ROOT_ID.toString(),
                        "category", categoryId.toString(), "created_at", PAST));

        assertEquals(categoryId, containerRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void create_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", "not-a-uuid", "name", "Invalid Id", "parent", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withInvalidUuidParent_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Invalid Parent", "parent", "not-a-uuid", "created_at", PAST), 400);
    }

    @Test
    public void create_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Future", "parent", ROOT_ID.toString(), "created_at", FUTURE), 400);
    }

    @Test
    public void create_withCreatedAtInPast_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Past", "parent", ROOT_ID.toString(), "created_at", PAST));
        assertTrue(containerRepository.findByIdOptional(id).isPresent());
    }

    @Test
    public void update_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("name", "No Id"), 400);
    }

    @Test
    public void update_withoutName_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Stable", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "description", "d"));

        assertEquals("Stable", containerRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void update_withoutCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "UCAT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Cat Container", "parent", ROOT_ID.toString(),
                        "category", categoryId.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Cat Container 2"));

        assertEquals(categoryId, containerRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void update_withoutDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Desc Keep", "parent", ROOT_ID.toString(), "description", "orig", "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Desc Keep 2"));

        assertEquals("orig", containerRepository.findByIdOptional(id).orElseThrow().description);
    }

    @Test
    public void update_withoutPosition_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Pos Keep", "parent", ROOT_ID.toString(), "position", "left", "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Pos Keep 2"));

        assertEquals("left", containerRepository.findByIdOptional(id).orElseThrow().position);
    }

    @Test
    public void update_onlyId_leavesSnapshotUnchanged() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Untouched", "parent", ROOT_ID.toString(),
                        "description", "d", "position", "p", "type", "BOX", "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString()));

        Container container = containerRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Untouched", container.name);
        assertEquals("d", container.description);
        assertEquals("p", container.position);
        assertEquals("BOX", container.type);
        assertEquals(ROOT_ID, container.parentContainer.id);
    }

    @Test
    public void update_withBlankName_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "   "), 400);
    }

    @Test
    public void update_withNameTwoChars_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Ab"), 400);
    }

    @Test
    public void update_withoutParent_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "name", "Valid 2"));

        assertEquals(ROOT_ID, containerRepository.findByIdOptional(id).orElseThrow().parentContainer.id);
    }

    @Test
    public void update_withNonExistentParent_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "parent", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void update_withNonExistentCategory_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "category", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void update_withExistingCategory_succeeds() {
        UUID head = currentHead();
        UUID categoryId = createCategory(head, "UPCT");
        head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_UPDATE", payload("id", id.toString(), "category", categoryId.toString()));

        assertEquals(categoryId, containerRepository.findByIdOptional(id).orElseThrow().category.id);
    }

    @Test
    public void update_withInvalidUuidCategory_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "category", "not-a-uuid"), 400);
    }

    @Test
    public void update_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", "not-a-uuid", "name", "X"), 400);
    }

    @Test
    public void update_withInvalidUuidParent_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "parent", "not-a-uuid"), 400);
    }

    @Test
    public void update_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "created_at", FUTURE), 400);
    }

    @Test
    public void update_withCreatedAtInPast_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "created_at", PAST), 400);
    }

    @Test
    public void update_withEmptyCreatedAt_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_CREATE", payload("id", id.toString(), "name", "Valid", "parent", ROOT_ID.toString(), "created_at", PAST));
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", id.toString(), "created_at", ""), 400);
    }

    @Test
    public void delete_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_DELETE", payload(), 400);
    }

    @Test
    public void delete_withNonExistentId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void create_withOnlyRequiredFields_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_CREATE",
                payload("id", id.toString(), "name", "Minimal", "parent", ROOT_ID.toString(), "created_at", PAST));

        Container container = containerRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Minimal", container.name);
        assertNull(container.description);
        assertNull(container.category);
        assertNull(container.type);
    }

    @Test
    public void root_cannotBeUpdated() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_UPDATE", payload("id", ROOT_ID.toString(), "name", "Hacked Root"), 400);
    }

    @Test
    public void root_cannotBeDeleted() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_DELETE", payload("id", ROOT_ID.toString()), 400);
    }
}
