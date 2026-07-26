package de.henzeob.inventory.api;

import de.henzeob.inventory.model.dto.CommandDTO;
import de.henzeob.inventory.model.dto.CommandEntryDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.ROOT_ID;
import static de.henzeob.inventory.support.SyncTestSupport.apply;
import static de.henzeob.inventory.support.SyncTestSupport.applyExpectStatus;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.applyRaw;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.payload;

/**
 * "Command Tests" section of DOMAIN-RULES.md: generic id / command_type / command_version
 * validation, independent of any single command_type's own field rules.
 */
@QuarkusTest
public class CommandValidationTest {

    private Map<String, Object> itemCreatePayload(UUID id) {
        return payload(
                "id", id.toString(),
                "name", "Version Test Item",
                "container", ROOT_ID.toString(),
                "quantity", 1,
                "created_at", PAST);
    }

    @Test
    public void commandVersion1_succeeds() {
        UUID head = currentHead();
        applyOk(head, "ITEM_CREATE", itemCreatePayload(UUID.randomUUID()));
    }

    @Test
    public void commandVersion0_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE", 0, itemCreatePayload(UUID.randomUUID()), 400);
    }

    @Test
    public void commandVersion2_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_CREATE", 2, itemCreatePayload(UUID.randomUUID()), 400);
    }

    @Test
    public void commandVersionMissing_fails() {
        UUID head = currentHead();
        apply(head, "ITEM_CREATE", null, itemCreatePayload(UUID.randomUUID())).then().statusCode(400);
    }

    @Test
    public void itemUpdate_versionVariants() {
        UUID head = currentHead();
        UUID itemId = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE", itemCreatePayload(itemId));

        applyExpectStatus(head, "ITEM_UPDATE", 0, payload("id", itemId.toString(), "name", "Renamed A"), 400);
        applyExpectStatus(head, "ITEM_UPDATE", 2, payload("id", itemId.toString(), "name", "Renamed B"), 400);
        apply(head, "ITEM_UPDATE", null, payload("id", itemId.toString(), "name", "Renamed C")).then().statusCode(400);
        applyOk(head, "ITEM_UPDATE", payload("id", itemId.toString(), "name", "Renamed D"));
    }

    @Test
    public void invalidCommandType_containerImageUpdate_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_IMAGE_UPDATE",
                payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void invalidCommandType_itemImageUpdate_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "ITEM_IMAGE_UPDATE",
                payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void missingCommandId_fails() {
        UUID head = currentHead();
        CommandDTO command = new CommandDTO();
        command.id = null;
        command.commandType = "ITEM_CREATE";
        command.commandVersion = 1;
        command.payload = itemCreatePayload(UUID.randomUUID());
        CommandEntryDTO entry = new CommandEntryDTO(head, null, command);
        applyRaw(head.toString(), List.of(entry)).then().statusCode(400);
    }

    @Test
    public void malformedCommandId_fails() {
        UUID head = currentHead();
        String body = "[{\"parent\":\"" + head + "\",\"child\":null,\"command\":{"
                + "\"id\":\"not-a-uuid\",\"command_type\":\"ITEM_CREATE\",\"command_version\":1,\"payload\":{}}}]";
        RestAssured.given()
                .contentType("application/json")
                .queryParam("head", head.toString())
                .body(body)
                .post("/api/v2/sync/applyCommands")
                .then().statusCode(400);
    }
}
