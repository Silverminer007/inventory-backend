package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ItemResourceTest {

    private String containerId;

    @BeforeEach
    void setupContainer() {
        if (containerId != null) return;
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
                .extract().jsonPath().getString("[0].entityId");
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
    public void testCreateItemNoAutoTagging() {
        // "roter Laptop" triggers: "Technik" (keyword "laptop") + "Farbe: Rot" (special rule "rot")
        String command = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Roter Laptop",
                    "description": "Ein roter Laptop",
                    "containerId": "%s",
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
                .body("[0].snapshot.tags", hasSize(0));
    }

    @Test
    public void testAddTagsToItem() {
        // Create item
        String createCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Laptop",
                    "containerId": "%s",
                    "quantity": 1,
                    "tags": ["Technik"]
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        String itemId = given()
                .contentType("application/json")
                .body(createCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasItem("Technik"))
                .extract().jsonPath().getString("[0].entityId");

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
                "entityId": "%s",
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
        // Create item
        String createCommand = """
            [{
                "commandId": "%s",
                "commandType": "ITEM_CREATE",
                "payload": {
                    "name": "Kleiner roter Laptop",
                    "containerId": "%s",
                    "quantity": 1,
                    "tags": ["Technik", "Klein", "Farbe: Rot"]
                }
            }]
        """.formatted(UUID.randomUUID(), containerId);

        String itemId = given()
                .contentType("application/json")
                .body(createCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.tags", hasSize(3))
                .body("[0].snapshot.tags", hasItems("Technik", "Klein", "Farbe: Rot"))
                .extract().jsonPath().getString("[0].entityId");

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
                "entityId": "%s",
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
