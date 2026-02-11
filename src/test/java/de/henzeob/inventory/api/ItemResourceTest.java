package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;
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
        String containerBody = """
            {
                "name": "Testraum",
                "containerType": "ROOM"
            }
        """;

        containerId = given()
                .contentType("application/json")
                .body(containerBody)
                .when().post("/api/v1/containers")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
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
    public void testCreateItem() {
        String requestBody = """
            {
                "name": "Test Laptop",
                "description": "Ein Test-Laptop",
                "containerId": null,
                "quantity": 1
            }
        """;

        // Note: This will fail because containerId is required
        // TODO: Setup test data first
        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/items")
                .then()
                .statusCode(400); // Expect validation error
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
        String requestBody = """
            {
                "name": "Roter Laptop",
                "description": "Ein roter Laptop",
                "containerId": %d,
                "quantity": 1
            }
        """.formatted(containerId);

        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/items")
                .then()
                .statusCode(201)
                .body("tags", hasSize(greaterThanOrEqualTo(2)));
    }

    @Test
    public void testCreateItemAutoTaggingExactlyThreeTags() {
        // "kleiner roter Laptop" triggers:
        //   "Technik" (keyword "laptop") + "Klein" (special rule "klein") + "Farbe: Rot" (special rule "rot")
        String requestBody = """
            {
                "name": "Kleiner roter Laptop",
                "description": "Ein kleiner roter Laptop",
                "containerId": %d,
                "quantity": 1
            }
        """.formatted(containerId);

        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/items")
                .then()
                .statusCode(201)
                .body("tags", hasSize(3));
    }

    @Test
    public void testAddTagsToItem() {
        // Create item — "Laptop" gets auto-tag "Technik"
        String createBody = """
            {
                "name": "Laptop",
                "containerId": %d,
                "quantity": 1
            }
        """.formatted(containerId);

        Long itemId = given()
                .contentType("application/json")
                .body(createBody)
                .when().post("/api/v1/items")
                .then()
                .statusCode(201)
                .body("tags", hasItem("Technik"))
                .extract().jsonPath().getLong("id");

        // Update: keep "Technik" and add two more tags
        String updateBody = """
            {
                "id": %d,
                "name": "Laptop",
                "containerId": %d,
                "quantity": 1,
                "tags": ["Technik", "Büro", "Arbeit"]
            }
        """.formatted(itemId, containerId);

        given()
                .contentType("application/json")
                .body(updateBody)
                .when().put("/api/v1/items/" + itemId)
                .then()
                .statusCode(200);

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
        String createBody = """
            {
                "name": "Kleiner roter Laptop",
                "containerId": %d,
                "quantity": 1
            }
        """.formatted(containerId);

        Long itemId = given()
                .contentType("application/json")
                .body(createBody)
                .when().post("/api/v1/items")
                .then()
                .statusCode(201)
                .body("tags", hasSize(3))
                .body("tags", hasItems("Technik", "Klein", "Farbe: Rot"))
                .extract().jsonPath().getLong("id");

        // Update: keep only "Technik", remove "Klein" and "Farbe: Rot"
        String updateBody = """
            {
                "id": %d,
                "name": "Kleiner roter Laptop",
                "containerId": %d,
                "quantity": 1,
                "tags": ["Technik"]
            }
        """.formatted(itemId, containerId);

        given()
                .contentType("application/json")
                .body(updateBody)
                .when().put("/api/v1/items/" + itemId)
                .then()
                .statusCode(200);

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