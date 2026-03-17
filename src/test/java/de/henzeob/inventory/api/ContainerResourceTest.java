package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
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
        String command = """
            [{
                "commandId": "%s",
                "commandType": "CONTAINER_CREATE",
                "payload": {
                    "containerType": "ROOM"
                }
            }]
        """.formatted(UUID.randomUUID());

        // Missing name — command should FAIL
        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("FAILED"));
    }

    @Test
    public void testDeleteNonExistentContainer() {
        String command = """
            [{
                "commandId": "%s",
                "commandType": "CONTAINER_DELETE",
                "entityId": "00000000-0000-0000-0000-000000000000",
                "payload": {
                    "version": 0
                }
            }]
        """.formatted(UUID.randomUUID());

        // Non-existent container — command should FAIL
        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("FAILED"));
    }
}
