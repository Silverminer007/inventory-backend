package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.ROOT_ID;
import static de.henzeob.inventory.support.SyncTestSupport.applyExpectStatus;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.applyRaw;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.fetchCommands;
import static de.henzeob.inventory.support.SyncTestSupport.payload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Sync" section of DOMAIN-RULES.md. */
@QuarkusTest
public class SyncTest {

    private Map<String, Object> containerCreatePayload(UUID id) {
        return payload(
                "id", id.toString(),
                "name", "Sync Test Container",
                "parent", ROOT_ID.toString(),
                "created_at", PAST);
    }

    @Test
    public void applyCommands_headLiteralNull_400() {
        RestAssured.given().contentType("application/json").body(List.of())
                .queryParam("head", "null")
                .post("/api/v2/sync/applyCommands")
                .then().statusCode(400);
    }

    @Test
    public void applyCommands_headMalformed_400() {
        RestAssured.given().contentType("application/json").body(List.of())
                .queryParam("head", "21313123")
                .post("/api/v2/sync/applyCommands")
                .then().statusCode(400);
    }

    @Test
    public void applyCommands_headMissing_400() {
        RestAssured.given().contentType("application/json").body(List.of())
                .post("/api/v2/sync/applyCommands")
                .then().statusCode(400);
    }

    @Test
    public void applyCommands_emptyPayload_isIdempotent() {
        UUID head = currentHead();
        Response first = applyRaw(head.toString(), List.of());
        first.then().statusCode(200);
        assertEquals(head.toString(), first.getBody().asString());

        Response second = applyRaw(head.toString(), List.of());
        second.then().statusCode(200);
        assertEquals(head.toString(), second.getBody().asString());
    }

    @Test
    public void applyCommands_invalidCommandPayload_400() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE", payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void applyCommands_invalidCommandType_400() {
        UUID head = currentHead();
        applyExpectStatus(head, "NOT_A_REAL_TYPE",
                containerCreatePayload(UUID.randomUUID()), 400);
    }

    @Test
    public void applyCommands_invalidCommandVersion_400() {
        UUID head = currentHead();
        applyExpectStatus(head, "CONTAINER_CREATE", 2, containerCreatePayload(UUID.randomUUID()), 400);
    }

    @Test
    public void applyCommands_oneValidOneInvalid_rejectsWholeBatch() {
        UUID head = currentHead();
        UUID validId = UUID.randomUUID();

        var validCommand = new de.henzeob.inventory.model.dto.CommandDTO();
        validCommand.id = UUID.randomUUID();
        validCommand.commandType = "CONTAINER_CREATE";
        validCommand.commandVersion = 1;
        validCommand.payload = containerCreatePayload(validId);
        var validEntry = new de.henzeob.inventory.model.dto.CommandEntryDTO(head, null, validCommand);

        var invalidCommand = new de.henzeob.inventory.model.dto.CommandDTO();
        invalidCommand.id = UUID.randomUUID();
        invalidCommand.commandType = "CONTAINER_CREATE";
        invalidCommand.commandVersion = 1;
        invalidCommand.payload = payload("id", UUID.randomUUID().toString()); // missing required fields
        var invalidEntry = new de.henzeob.inventory.model.dto.CommandEntryDTO(validCommand.id, null, invalidCommand);

        applyRaw(head.toString(), List.of(validEntry, invalidEntry)).then().statusCode(400);

        assertEquals(head, currentHead(), "head must not move when any command in the batch fails");
    }

    @Test
    public void applyCommands_staleHead_409() {
        UUID head = currentHead();
        UUID newHead = applyOk(head, "CONTAINER_CREATE", containerCreatePayload(UUID.randomUUID()));
        assertTrue(!newHead.equals(head));

        applyExpectStatus(head, "CONTAINER_CREATE", containerCreatePayload(UUID.randomUUID()), 409);
    }

    @Test
    public void applyCommands_parentDoesNotMatchHead_409() {
        // Move the chain forward so the real tip is no longer the root.
        UUID head = currentHead();
        UUID realHead = applyOk(head, "CONTAINER_CREATE", containerCreatePayload(UUID.randomUUID()));
        assertFalse(realHead.equals(ROOT_ID));

        // head query param is the real tip, but the command's parent points at the root instead.
        var command = new de.henzeob.inventory.model.dto.CommandDTO();
        command.id = UUID.randomUUID();
        command.commandType = "CONTAINER_CREATE";
        command.commandVersion = 1;
        command.payload = containerCreatePayload(UUID.randomUUID());
        var entry = new de.henzeob.inventory.model.dto.CommandEntryDTO(ROOT_ID, null, command);

        applyRaw(realHead.toString(), List.of(entry)).then().statusCode(409);

        assertEquals(realHead, currentHead(), "head must not move when the batch is rejected");
    }

    @Test
    public void fetchCommands_sinceLiteralNull_400() {
        RestAssured.given().queryParam("since", "null").get("/api/v2/sync/fetchCommands")
                .then().statusCode(400);
    }

    @Test
    public void fetchCommands_sinceMalformed_400() {
        RestAssured.given().queryParam("since", "123123").get("/api/v2/sync/fetchCommands")
                .then().statusCode(400);
    }

    @Test
    public void fetchCommands_sinceNonExistent_404() {
        RestAssured.given().queryParam("since", "99999999-9999-9999-9999-999999999999")
                .get("/api/v2/sync/fetchCommands")
                .then().statusCode(404);
    }

    @Test
    public void fetchCommands_sinceRoot_200() {
        fetchCommands(ROOT_ID.toString()).then().statusCode(200);
    }

    @Test
    public void fetchCommands_noSince_200() {
        fetchCommands(null).then().statusCode(200);
    }

    @Test
    public void multiStep_appliedCommandBecomesChildOfHead() {
        UUID head = currentHead();
        UUID newId = UUID.randomUUID();
        UUID newHead = applyOk(head, "CONTAINER_CREATE", containerCreatePayload(newId));

        List<Map<String, Object>> entries = fetchCommands(ROOT_ID.toString()).jsonPath().getList("$");
        Map<String, Object> headEntry = entries.stream()
                .filter(e -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> command = (Map<String, Object>) e.get("command");
                    return head.toString().equals(command.get("id"));
                })
                .findFirst()
                .orElseThrow();

        assertEquals(newHead.toString(), headEntry.get("child"));
        assertFalse(entries.isEmpty());
    }
}
