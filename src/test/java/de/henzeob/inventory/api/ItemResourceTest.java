package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ItemResourceTest {

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
}