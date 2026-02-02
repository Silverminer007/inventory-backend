package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ContainerResourceTest {

    @Test
    public void testGetAllContainersEndpoint() {
        given()
                .when().get("/api/v1/containers")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testGetRootContainersEndpoint() {
        given()
                .when().get("/api/v1/containers/roots")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testCreateContainerWithoutName() {
        String requestBody = """
            {
                "containerType": "ROOM"
            }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/containers")
                .then()
                .statusCode(400);
    }

    @Test
    public void testDeleteNonExistentContainer() {
        given()
                .when().delete("/api/v1/containers/999999")
                .then()
                .statusCode(404);
    }
}
