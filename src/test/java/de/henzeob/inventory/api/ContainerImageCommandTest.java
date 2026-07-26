package de.henzeob.inventory.api;

import de.henzeob.inventory.model.entity.Image;
import de.henzeob.inventory.repository.ImageRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Container Image Tests" section of DOMAIN-RULES.md. */
@QuarkusTest
public class ContainerImageCommandTest {

    @Inject
    ImageRepository imageRepository;

    @Test
    public void create_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_IMAGE_CREATE",
                payload("id", id.toString(), "container", ROOT_ID.toString(), "created_at", PAST));

        Image image = imageRepository.findByIdOptional(id).orElseThrow();
        assertEquals(ROOT_ID, image.container.id);
    }

    @Test
    public void create_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CONTAINER_IMAGE_CREATE", payload("id", id.toString(), "container", ROOT_ID.toString(), "created_at", PAST));
        applyOk(head, "CONTAINER_IMAGE_DELETE", payload("id", id.toString()));

        assertTrue(imageRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void create_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("container", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withoutContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("id", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withNonExistentContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("id", UUID.randomUUID().toString(), "container", UUID.randomUUID().toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("id", "not-a-uuid", "container", ROOT_ID.toString(), "created_at", PAST), 400);
    }

    @Test
    public void create_withInvalidUuidContainer_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("id", UUID.randomUUID().toString(), "container", "not-a-uuid", "created_at", PAST), 400);
    }

    @Test
    public void create_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_CREATE",
                payload("id", UUID.randomUUID().toString(), "container", ROOT_ID.toString(), "created_at", FUTURE), 400);
    }

    @Test
    public void create_withCreatedAtInPast_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CONTAINER_IMAGE_CREATE",
                payload("id", id.toString(), "container", ROOT_ID.toString(), "created_at", PAST));
        assertTrue(imageRepository.findByIdOptional(id).isPresent());
    }

    @Test
    public void delete_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_DELETE", payload(), 400);
    }

    @Test
    public void delete_withNonExistentId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_DELETE", payload("id", UUID.randomUUID().toString()), 400);
    }
}
