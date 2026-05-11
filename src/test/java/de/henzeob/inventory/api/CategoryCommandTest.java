package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CategoryCommandTest {

    private String defaultCategoryId;
    private String roomId;

    @BeforeEach
    void setup() {
        if (defaultCategoryId != null) return;

        defaultCategoryId = given().get("/api/v1/categories/by-short-code/XX")
                .then().statusCode(200)
                .extract().jsonPath().getString("id");

        roomId = postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Category Test Room","containerType":"ROOM"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");
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

    private String createCategory(String name, String shortCode) {
        return postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_CREATE",
              "payload":{"name":"%s","shortCode":"%s"}}]
            """.formatted(UUID.randomUUID(), name, shortCode))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");
    }

    private long categoryVersion(String id) {
        return given().get("/api/v1/categories/" + id)
                .then().statusCode(200)
                .extract().jsonPath().getLong("version");
    }

    private long advanceCategoryVersion(String id, String newName) {
        long ver = categoryVersion(id);
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"%s","version":%d}}]
            """.formatted(UUID.randomUUID(), id, newName, ver))
                .body("[0].status", is("APPLIED"));
        return categoryVersion(id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CATEGORY_CREATE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void categoryCreate_applied() {
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_CREATE",
              "payload":{"name":"Electronics","shortCode":"EL1"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("APPLIED"))
                .body("[0].entityId", notNullValue())
                .body("[0].snapshot.name", is("Electronics"))
                .body("[0].snapshot.shortCode", is("EL1"))
                .body("[0].snapshot.version", notNullValue())
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryCreate_missingName_failed() {
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_CREATE",
              "payload":{"shortCode":"NO_NAME"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("FAILED"));
    }

    @Test
    void categoryCreate_missingShortCode_failed() {
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_CREATE",
              "payload":{"name":"No ShortCode Category"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("FAILED"));
    }

    @Test
    void categoryCreate_duplicateShortCode_failed() {
        // shortCode "XX" already exists from migration seed
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_CREATE",
              "payload":{"name":"Duplicate","shortCode":"XX"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("FAILED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CATEGORY_UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void categoryUpdate_matchingVersion_applied() {
        String id = createCategory("Update Target", "UT1");
        long ver = categoryVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Update Target Modified","version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.name", is("Update Target Modified"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryUpdate_noVersion_applied_legacyBehavior() {
        String id = createCategory("Legacy Update", "LU1");
        advanceCategoryVersion(id, "Legacy Update v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Legacy Update Final"}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryUpdate_staleVersion_conflictingName_conflict() {
        String id = createCategory("Conflict Cat", "CCF1");
        long staleVer = categoryVersion(id);
        advanceCategoryVersion(id, "Conflict Cat Updated");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Conflict Cat Old","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("name"))
                .body("[0].conflictInfo.serverSnapshot.name", is("Conflict Cat Updated"))
                .body("[0].conflictInfo.clientPayload.name", is("Conflict Cat Old"));
    }

    @Test
    void categoryUpdate_staleVersion_autoMerge_applied() {
        String id = createCategory("Merge Cat", "MCG1");
        long staleVer = categoryVersion(id);
        advanceCategoryVersion(id, "Merge Cat v2");

        // Client sends the server's current name — no real conflict
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Merge Cat v2","version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryUpdate_staleVersion_force_applied() {
        String id = createCategory("Force Cat", "FC1");
        long staleVer = categoryVersion(id);
        advanceCategoryVersion(id, "Force Cat v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Force Cat Override","version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryUpdate_nonExistent_failed() {
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_UPDATE",
              "entityId":"00000000-0000-0000-0000-000000000000",
              "payload":{"name":"Ghost"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("FAILED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CATEGORY_DELETE
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void categoryDelete_matchingVersion_applied() {
        String id = createCategory("Delete Me", "DM1");
        long ver = categoryVersion(id);

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_DELETE",
              "entityId":"%s",
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, ver))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void categoryDelete_noVersion_applied_legacyBehavior() {
        String id = createCategory("Delete Legacy", "DL1");
        advanceCategoryVersion(id, "Delete Legacy v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_DELETE",
              "entityId":"%s",
              "payload":{}}]
            """.formatted(UUID.randomUUID(), id))
                .body("[0].status", is("APPLIED"))
                .body("[0].conflictInfo", nullValue());
    }

    @Test
    void categoryDelete_staleVersion_conflict() {
        String id = createCategory("Delete Stale", "DS1");
        long staleVer = categoryVersion(id);
        long serverVer = advanceCategoryVersion(id, "Delete Stale v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_DELETE",
              "entityId":"%s",
              "payload":{"version":%d}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.clientVersion", is((int) staleVer))
                .body("[0].conflictInfo.serverVersion", is((int) serverVer))
                .body("[0].conflictInfo.conflictingFields", empty())
                .body("[0].conflictInfo.serverSnapshot", notNullValue());
    }

    @Test
    void categoryDelete_staleVersion_force_applied() {
        String id = createCategory("Delete Force", "DF1");
        long staleVer = categoryVersion(id);
        advanceCategoryVersion(id, "Delete Force v2");

        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_DELETE",
              "entityId":"%s",
              "payload":{"version":%d,"force":true}}]
            """.formatted(UUID.randomUUID(), id, staleVer))
                .body("[0].status", is("APPLIED"));
    }

    @Test
    void categoryDelete_nonExistent_failed() {
        postCommand("""
            [{"commandId":"%s","commandType":"CATEGORY_DELETE",
              "entityId":"00000000-0000-0000-0000-000000000000",
              "payload":{}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("FAILED"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Category wiring on ITEM
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void itemCreate_withCategory_categoryInSnapshot() {
        String catId = createCategory("Kitchen", "KI1");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"Toaster","containerId":"%s","quantity":1,
                         "category":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), roomId, catId))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.category.id", is(catId))
                .body("[0].snapshot.category.shortCode", is("KI1"));
    }

    @Test
    void itemCreate_withoutCategory_usesDefaultCategory() {
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"Uncategorized Item","containerId":"%s","quantity":1}}]
            """.formatted(UUID.randomUUID(), roomId))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.category.id", is(defaultCategoryId))
                .body("[0].snapshot.category.shortCode", is("XX"));
    }

    @Test
    void itemUpdate_changeCategory_applied() {
        String cat1Id = createCategory("Garden", "GA1");
        String cat2Id = createCategory("Garage", "GR1");

        String itemId = postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"Shovel","containerId":"%s","quantity":1,
                         "category":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), roomId, cat1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");

        long ver = given().get("/api/v1/items/" + itemId)
                .then().statusCode(200).extract().jsonPath().getLong("version");

        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Shovel","quantity":1,"category":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), itemId, cat2Id, ver))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.category.id", is(cat2Id));
    }

    @Test
    void itemUpdate_staleVersion_conflictingCategory_conflict() {
        String cat1Id = createCategory("Office", "OF1");
        String cat2Id = createCategory("Bedroom", "BD1");
        String cat3Id = createCategory("Hallway", "HW1");

        String itemId = postCommand("""
            [{"commandId":"%s","commandType":"ITEM_CREATE",
              "payload":{"name":"Lamp","containerId":"%s","quantity":1,
                         "category":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), roomId, cat1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");

        long staleVer = given().get("/api/v1/items/" + itemId)
                .then().statusCode(200).extract().jsonPath().getLong("version");

        // Server changes category to cat2
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Lamp","quantity":1,"category":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), itemId, cat2Id, staleVer))
                .body("[0].status", is("APPLIED"));

        // Client (stale) tries to set cat3 — should conflict
        postCommand("""
            [{"commandId":"%s","commandType":"ITEM_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Lamp","quantity":1,"category":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), itemId, cat3Id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("category"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Category wiring on CONTAINER
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void containerCreate_withPrimaryCategory_categoryInSnapshot() {
        String catId = createCategory("Storage", "ST1");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Storage Room","containerType":"ROOM",
                         "primaryCategory":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), catId))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.primaryCategory.id", is(catId))
                .body("[0].snapshot.primaryCategory.shortCode", is("ST1"));
    }

    @Test
    void containerCreate_withoutPrimaryCategory_usesDefaultCategory() {
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Uncategorized Room","containerType":"ROOM"}}]
            """.formatted(UUID.randomUUID()))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.primaryCategory.id", is(defaultCategoryId))
                .body("[0].snapshot.primaryCategory.shortCode", is("XX"));
    }

    @Test
    void containerUpdate_changePrimaryCategory_applied() {
        String cat1Id = createCategory("Workshop", "WS1");
        String cat2Id = createCategory("Utility", "UT2");

        String containerId = postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Workshop Room","containerType":"ROOM",
                         "primaryCategory":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), cat1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");

        long ver = given().get("/api/v1/containers/" + containerId)
                .then().statusCode(200).extract().jsonPath().getLong("version");

        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Workshop Room","primaryCategory":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), containerId, cat2Id, ver))
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.primaryCategory.id", is(cat2Id));
    }

    @Test
    void containerUpdate_staleVersion_conflictingPrimaryCategory_conflict() {
        String cat1Id = createCategory("Basement", "BM1");
        String cat2Id = createCategory("Attic", "AT1");
        String cat3Id = createCategory("Porch", "PR1");

        String containerId = postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_CREATE",
              "payload":{"name":"Multi Cat Room","containerType":"ROOM",
                         "primaryCategory":{"id":"%s"}}}]
            """.formatted(UUID.randomUUID(), cat1Id))
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getString("[0].entityId");

        long staleVer = given().get("/api/v1/containers/" + containerId)
                .then().statusCode(200).extract().jsonPath().getLong("version");

        // Server changes to cat2
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Multi Cat Room","primaryCategory":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), containerId, cat2Id, staleVer))
                .body("[0].status", is("APPLIED"));

        // Client (stale) tries to set cat3 — should conflict
        postCommand("""
            [{"commandId":"%s","commandType":"CONTAINER_UPDATE",
              "entityId":"%s",
              "payload":{"name":"Multi Cat Room","primaryCategory":{"id":"%s"},"version":%d}}]
            """.formatted(UUID.randomUUID(), containerId, cat3Id, staleVer))
                .body("[0].status", is("CONFLICT"))
                .body("[0].conflictInfo.conflictingFields", contains("primaryCategory"));
    }
}