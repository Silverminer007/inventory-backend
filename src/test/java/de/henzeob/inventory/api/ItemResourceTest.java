package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ItemResourceTest {

    private Long containerId;

    @BeforeAll
    void setupContainer() {
        String command = """
            [{
                "commandId": "%s",
                "commandType": "CONTAINER_CREATE",
                "payload": {
                    "name": "Testraum",
                    "containerType": "ROOM"
                }
            }]
        """.formatted(UUID.randomUUID());

        containerId = given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .extract().jsonPath().getLong("[0].entityId");
    }

    @Test
    public void testGetAllItemsEndpoint() {
        given()
                .when().get("/api/v1/items")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testCreateItemMissingContainerId() {
        String command = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Test Laptop",
                    "description": "Ein Test-Laptop",
                    "quantity": 1
                }
            }]
        """.formatted(UUID.randomUUID());

        // containerId is required — command should FAIL
        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("FAILED"));
    }

    @Test
    public void testSearchItems() {
        given()
                .queryParam("q", "laptop")
                .when().get("/api/v1/items/search")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testCreateItemAutoTaggingAtLeastTwoTags() {
        // "roter Laptop" triggers: "Technik" (keyword "laptop") + "Farbe: Rot" (special rule "rot")
        String command = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Roter Laptop",
                    "description": "Ein roter Laptop",
                    "containerId": %d,
                    "quantity": 1
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasSize(greaterThanOrEqualTo(2)));
    }

    @Test
    public void testCreateItemAutoTaggingExactlyThreeTags() {
        // "kleiner roter Laptop" triggers:
        //   "Technik" (keyword "laptop") + "Klein" (special rule "klein") + "Farbe: Rot" (special rule "rot")
        String command = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Kleiner roter Laptop",
                    "description": "Ein kleiner roter Laptop",
                    "containerId": %d,
                    "quantity": 1
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasSize(3));
    }

    @Test
    public void testAddTagsToItem() {
        // Create item — "Laptop" gets auto-tag "Technik"
        String createCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Laptop",
                    "containerId": %d,
                    "quantity": 1
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        Long itemId = given()
                .contentType("application/json")
                .body(createCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasItem("Technik"))
                .extract().jsonPath().getLong("[0].entityId");

        // Update: keep "Technik" and add two more tags
        // First get current version
        Long version = given()
                .when().get("/api/v1/items/" + itemId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getLong("version");

        String updateCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_UPDATE",
                "entityId": %d,
                "payload": {
                    "name": "Laptop",
                    "quantity": 1,
                    "tags": ["Technik", "Büro", "Arbeit"],
                    "version": %d
                }
            }]
        """.formatted(UUID.randomUUID(), itemId, version);

        given()
                .contentType("application/json")
                .body(updateCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"));

        // Verify via GET
        given()
                .when().get("/api/v1/items/" + itemId)
                .then()
                .statusCode(200)
                .body("tags", hasSize(3))
                .body("tags", hasItems("Technik", "Büro", "Arbeit"));
    }

    @Test
    public void testRemoveTagsFromItem() {
        // Create item — "Kleiner roter Laptop" gets 3 auto-tags
        String createCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Kleiner roter Laptop",
                    "containerId": %d,
                    "quantity": 1
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        Long itemId = given()
                .contentType("application/json")
                .body(createCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasSize(3))
                .body("[0].snapshot.tags", hasItems("Technik", "Klein", "Farbe: Rot"))
                .extract().jsonPath().getLong("[0].entityId");

        // Get current version
        Long version = given()
                .when().get("/api/v1/items/" + itemId)
                .then()
                .statusCode(200)
                .extract().jsonPath().getLong("version");

        // Update: keep only "Technik", remove "Klein" and "Farbe: Rot"
        String updateCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_UPDATE",
                "entityId": %d,
                "payload": {
                    "name": "Kleiner roter Laptop",
                    "quantity": 1,
                    "tags": ["Technik"],
                    "version": %d
                }
            }]
        """.formatted(UUID.randomUUID(), itemId, version);

        given()
                .contentType("application/json")
                .body(updateCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"));

        // Verify via GET
        given()
                .when().get("/api/v1/items/" + itemId)
                .then()
                .statusCode(200)
                .body("tags", hasSize(1))
                .body("tags", hasItem("Technik"))
                .body("tags", not(hasItem("Klein")))
                .body("tags", not(hasItem("Farbe: Rot")));
    }
}
