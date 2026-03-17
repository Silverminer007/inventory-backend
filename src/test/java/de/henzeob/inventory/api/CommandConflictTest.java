package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;

/**
 * Integration tests for version-based conflict detection in the Command API.
 *
 * Three-tier logic per command:
 *   - No version in payload   → APPLIED (legacy / backward-compat)
 *   - Version matches server  → APPLIED
 *   - Stale version + no field differences (UPDATE) → APPLIED (auto-merge)
 *   - Stale version + field differs (UPDATE)        → CONFLICT
 *   - Stale version (DELETE / MOVE)                 → CONFLICT
 *   - force=true in payload   → APPLIED regardless of version
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommandConflictTest {

    // Shared containers created once; never mutated by tests
    private Long room1Id;
    private Long room2Id;

    @BeforeAll
    void createBaseContainers() {
        room1Id = postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Conflict Test Room 1","containerType":"ROOM"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getLong("[0].entityId");

        room2Id = postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Conflict Test Room 2","containerType":"ROOM"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getLong("[0].entityId");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ValidatableResponse postCommand(String json) {
        return given()
                .contentType("application/json")
                .body(json)
                .when().post("/commands")
                .then()
                .statusCode(200);
    }

    /** Creates a fresh item in room1 and returns its id. */
    private long createItem(String name) {
        return postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"%s","containerId":%d,"quantity":1}}]
            """.formatted(UUID.randomUUID(), name, room1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getLong("[0].entityId");
    }

    /** Returns the current version of an item. */
    private long itemVersion(long itemId) {
        return given().get("/api/v1/items/" + itemId)
                .then().statusCode(200)
                .extract().jsonPath().getLong("version");
    }

    /** Creates a fresh SHELF inside room1 and returns its id. */
    private long createShelf(String name) {
        return postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"%s","containerType":"SHELF","parentContainerId":%d}}]
            """.formatted(UUID.randomUUID(), name, room1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getLong("[0].entityId");
    }

    /** Returns the current version of a container. */
    private long containerVersion(long containerId) {
        return given().get("/api/v1/containers/" + containerId)
                .then().statusCode(200)
                .extract().jsonPath().getLong("version");
    }

    /** Advances an item's version by renaming it once with the current version. */
    private long advanceItemVersion(long itemId, String newName) {
        long ver = itemVersion(itemId);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"%s","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), itemId, newName, ver))
                .body("[0].status", is("APPLIED"));
        return itemVersion(itemId);   // return new (advanced) version
    }

    /** Advances a container's version by renaming it once with the current version. */
    private long advanceContainerVersion(long containerId, String newName) {
        long ver = containerVersion(containerId);
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"%s","version":%d}}]
            """.formatted(UUID.randomUUID(), containerId, newName, ver))
                .body("[0].status", is("APPLIED"));
        return containerVersion(containerId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ITEM_UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemUpdate_matchingVersion_applied() {
        long id = createItem("Laptop");
        long ver = itemVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Laptop Pro","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemUpdate_noVersion_applied_legacyBehavior() {
        long id = createItem("Tablet");
        advanceItemVersion(id, "Tablet Gen2");  // version is now 1+

        // No version field in payload → always applied (backward compat)
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Tablet Legacy","quantity":1}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemUpdate_staleVersion_conflictingName_conflict() {
        long id = createItem("Monitor");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Monitor 4K");   // server advances to next version

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Monitor HD","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.clientVersion", is((int) staleVer))
                .body("[0].conflictInfo.serverVersion", is((int) staleVer + 1))
                .body("[0].conflictInfo.conflictingFields", contains("name"))
                .body("[0].conflictInfo.serverSnapshot.name", is("Monitor 4K"))
                .body("[0].conflictInfo.clientPayload.name", is("Monitor HD"))
                .body("[0].snapshot", nullValue())
                .body("[0].serverSequence", nullValue());
    }

    @Test
    void itemUpdate_staleVersion_autoMerge_allFieldsMatchServer_applied() {
        long id = createItem("Keyboard");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Keyboard Wireless");  // server name is now "Keyboard Wireless"

        // Client sends the server's current name with an old version → auto-merge
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Keyboard Wireless","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemUpdate_staleVersion_force_applied() {
        long id = createItem("Mouse");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Mouse Wireless");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Mouse Force","quantity":1,"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemUpdate_staleVersion_multipleConflictingFields() {
        long id = createItem("Headphones");
        long staleVer = itemVersion(id);

        // Server (Client A) changes BOTH name AND description in one command
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Headphones Pro","description":"Pro version","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B sends different name AND different description with stale version
        // → both fields were also changed server-side → CONFLICT on both
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Headphones Cheap","description":"Billig","quantity":1,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", hasItem("name"))
                .body("[0].conflictInfo.conflictingFields", hasItem("description"));
    }

    @Test
    void itemUpdate_staleVersion_tagsConflict() {
        long id = createItem("Speaker");
        long staleVer = itemVersion(id);

        // Advance version and update tags on server
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Speaker","quantity":1,"tags":["Musik","Audio"],"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client sends different tag set with stale version
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Speaker","quantity":1,"tags":["OldTag"],"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("tags"));
    }

    @Test
    void itemUpdate_staleVersion_tagsAutoMerge_applied() {
        long id = createItem("Webcam");
        long ver = itemVersion(id);

        // Set tags with correct version
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Webcam","quantity":1,"tags":["Video","Stream"],"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        long staleVer = ver;  // version before the tag update

        // Client sends same tags as server, stale version → auto-merge
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Webcam","quantity":1,"tags":["Video","Stream"],"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemUpdate_conflict_doesNotPersistCommandRow() {
        // After a CONFLICT the command row must be deleted — submitting the same commandId again
        // must re-evaluate, not return a cached APPLIED result.
        long id = createItem("Printer");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Printer Pro");

        String fixedCommandId = UUID.randomUUID().toString();
        String conflictPayload = """
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Printer Old","quantity":1,"version":%d}}]
            """.formatted(fixedCommandId, id, staleVer);

        // First call → CONFLICT
        postCommand(conflictPayload).body("[0].status", is("CONFLICT"));

        // Retry same commandId → still CONFLICT (not replayed as APPLIED from DB)
        postCommand(conflictPayload).body("[0].status", is("CONFLICT"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ITEM_DELETE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemDelete_matchingVersion_applied() {
        long id = createItem("USB Hub");
        long ver = itemVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_DELETE",
              "entityId":%d,
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void itemDelete_noVersion_applied_legacyBehavior() {
        long id = createItem("Extension Cord");
        advanceItemVersion(id, "Extension Cord 2m");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_DELETE",
              "entityId":%d,
              "payload":{}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemDelete_staleVersion_conflict() {
        long id = createItem("Power Strip");
        long staleVer = itemVersion(id);
        long serverVer = advanceItemVersion(id, "Power Strip Surge");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_DELETE",
              "entityId":%d,
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.clientVersion", is((int) staleVer))
                .body("[0].conflictInfo.serverVersion", is((int) serverVer))
                .body("[0].conflictInfo.conflictingFields", empty())
                .body("[0].conflictInfo.serverSnapshot", notNullValue());
    }

    @Test
    void itemDelete_staleVersion_force_applied() {
        long id = createItem("Battery");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Battery AA");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_DELETE",
              "entityId":%d,
              "payload":{"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ITEM_MOVE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemMove_matchingVersion_applied() {
        long id = createItem("Charger");
        long ver = itemVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_MOVE",
              "entityId":%d,
              "payload":{"containerId":%d,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id, ver))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void itemMove_noVersion_applied_legacyBehavior() {
        long id = createItem("Adapter");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_MOVE",
              "entityId":%d,
              "payload":{"containerId":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void itemMove_staleVersion_conflict() {
        long id = createItem("Cable");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Cable HDMI");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_MOVE",
              "entityId":%d,
              "payload":{"containerId":%d,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", empty())
                .body("[0].conflictInfo.serverSnapshot", notNullValue());
    }

    @Test
    void itemMove_staleVersion_force_applied() {
        long id = createItem("Dock");
        long staleVer = itemVersion(id);
        advanceItemVersion(id, "Dock Thunderbolt");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_MOVE",
              "entityId":%d,
              "payload":{"containerId":%d,"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, room2Id, staleVer))
                .body("[0].status", is("APPLIED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTAINER_UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void containerUpdate_matchingVersion_applied() {
        long id = createShelf("Shelf Alpha");
        long ver = containerVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Shelf Alpha Updated","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerUpdate_noVersion_applied_legacyBehavior() {
        long id = createShelf("Shelf Beta");
        advanceContainerVersion(id, "Shelf Beta v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Shelf Beta Legacy"}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerUpdate_staleVersion_conflictingName_conflict() {
        long id = createShelf("Shelf Gamma");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Gamma v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Shelf Gamma Old","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("name"))
                .body("[0].conflictInfo.serverSnapshot.name", is("Shelf Gamma v2"))
                .body("[0].conflictInfo.clientPayload.name", is("Shelf Gamma Old"));
    }

    @Test
    void containerUpdate_staleVersion_autoMerge_applied() {
        long id = createShelf("Shelf Delta");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Delta v2");

        // Client sends the server's current name → auto-merge
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Shelf Delta v2","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerUpdate_staleVersion_force_applied() {
        long id = createShelf("Shelf Epsilon");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Epsilon v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Shelf Epsilon Force","version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTAINER_DELETE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void containerDelete_matchingVersion_applied() {
        long id = createShelf("Shelf Delete Match");
        long ver = containerVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_DELETE",
              "entityId":%d,
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void containerDelete_noVersion_applied_legacyBehavior() {
        long id = createShelf("Shelf Delete Legacy");
        advanceContainerVersion(id, "Shelf Delete Legacy v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_DELETE",
              "entityId":%d,
              "payload":{}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerDelete_staleVersion_conflict() {
        long id = createShelf("Shelf Delete Stale");
        long staleVer = containerVersion(id);
        long serverVer = advanceContainerVersion(id, "Shelf Delete Stale v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_DELETE",
              "entityId":%d,
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.clientVersion", is((int) staleVer))
                .body("[0].conflictInfo.serverVersion", is((int) serverVer))
                .body("[0].conflictInfo.conflictingFields", empty())
                .body("[0].conflictInfo.serverSnapshot", notNullValue());
    }

    @Test
    void containerDelete_staleVersion_force_applied() {
        long id = createShelf("Shelf Delete Force");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Delete Force v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_DELETE",
              "entityId":%d,
              "payload":{"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTAINER_MOVE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void containerMove_matchingVersion_applied() {
        long id = createShelf("Shelf Move Match");
        long ver = containerVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_MOVE",
              "entityId":%d,
              "payload":{"newParentContainerId":%d,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id, ver))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void containerMove_noVersion_applied_legacyBehavior() {
        long id = createShelf("Shelf Move Legacy");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_MOVE",
              "entityId":%d,
              "payload":{"newParentContainerId":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerMove_staleVersion_conflict() {
        long id = createShelf("Shelf Move Stale");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Move Stale v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_MOVE",
              "entityId":%d,
              "payload":{"newParentContainerId":%d,"version":%d}}]
            """.formatted(UUID.randomUUID(), id, room2Id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", empty())
                .body("[0].conflictInfo.serverSnapshot", notNullValue());
    }

    @Test
    void containerMove_staleVersion_force_applied() {
        long id = createShelf("Shelf Move Force");
        long staleVer = containerVersion(id);
        advanceContainerVersion(id, "Shelf Move Force v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_MOVE",
              "entityId":%d,
              "payload":{"newParentContainerId":%d,"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, room2Id, staleVer))
                .body("[0].status", is("APPLIED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CREATE commands — no version tracking, always APPLIED
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemCreate_noConflictCheck_alwaysApplied() {
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"New Item No Conflict","containerId":%d,"quantity":1}}]
            """.formatted(UUID.randomUUID(), room1Id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void containerCreate_noConflictCheck_alwaysApplied() {
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"New Container No Conflict","containerType":"SHELF",
                         "parentContainerId":%d}}]
            """.formatted(UUID.randomUUID(), room1Id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3-way merge: non-overlapping field changes auto-merge
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemUpdate_serverChangedName_clientChangedDescription_autoMerge() {
        long id = createItem("Widget");
        long staleVer = itemVersion(id);

        // Client A (server-side): only changes name
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Widget Pro","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B (offline, stale): only changes description — non-overlapping with Client A
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"description":"Client B description","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue())
                .body("[0].snapshot.name", is("Widget Pro"))
                .body("[0].snapshot.description", is("Client B description"));

        // Verify merged state via GET
        given().get("/api/v1/items/" + id)
                .then().statusCode(200)
                .body("name", is("Widget Pro"))
                .body("description", is("Client B description"));
    }

    @Test
    void itemUpdate_serverChangedName_clientChangedSameName_conflict() {
        long id = createItem("Gadget");
        long staleVer = itemVersion(id);

        // Server (Client A): renames item
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Gadget Pro","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B: also renames item to something different — overlap on "name"
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Gadget Max","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("name"));
    }

    @Test
    void itemUpdate_serverChangedName_clientChangedDescriptionAndName_onlyNameConflicts() {
        long id = createItem("Tool");
        long staleVer = itemVersion(id);

        // Server: renames item
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Tool Pro","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B: changes name (conflicts) AND description (server didn't touch it → auto-merge for description)
        // Result must be CONFLICT with only "name" listed — description can be merged but the whole command
        // is still blocked because name conflicts
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Tool Max","description":"My description","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("name"));
    }

    @Test
    void itemUpdate_serverChangedMultipleFields_clientChangedDisjointField_autoMerge() {
        long id = createItem("Device");
        long staleVer = itemVersion(id);

        // Server (Client A): changes name and position — two fields
        long ver = itemVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"name":"Device Pro","position":"Shelf 3","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B (stale): changes only barcode — completely disjoint from server changes
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":%d,
              "payload":{"barcode":"DEV-001","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue())
                .body("[0].snapshot.name", is("Device Pro"))
                .body("[0].snapshot.position", is("Shelf 3"))
                .body("[0].snapshot.barcode", is("DEV-001"));
    }

    @Test
    void containerUpdate_serverChangedName_clientChangedDescription_autoMerge() {
        long id = createShelf("Container Widget");
        long staleVer = containerVersion(id);

        // Client A (server-side): only changes name
        long ver = containerVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Container Widget Pro","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B (offline, stale): only changes description — non-overlapping
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"description":"Client B desc","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue())
                .body("[0].snapshot.name", is("Container Widget Pro"))
                .body("[0].snapshot.description", is("Client B desc"));

        // Verify merged state via GET
        given().get("/api/v1/containers/" + id)
                .then().statusCode(200)
                .body("name", is("Container Widget Pro"))
                .body("description", is("Client B desc"));
    }

    @Test
    void containerUpdate_serverChangedName_clientChangedSameName_conflict() {
        long id = createShelf("Container Gadget");
        long staleVer = containerVersion(id);

        // Server: renames container
        long ver = containerVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Container Gadget Pro","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));

        // Client B: also renames to something different — overlap on "name"
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":%d,
              "payload":{"name":"Container Gadget Max","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("name"));
    }
}
