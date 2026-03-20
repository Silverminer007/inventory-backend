package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ImageResourceTest {

    @Test
    public void testGetImagesForNonExistentItem() {
        given()
                .when().get("/api/v1/images/items/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetImagesForNonExistentContainer() {
        given()
                .when().get("/api/v1/images/containers/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetNonExistentImage() {
        given()
                .when().get("/api/v1/images/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteNonExistentImage() {
        String command = """
            [{
                "commandId": "%s",
                "commandType": "IMAGE_DELETE",
                "entityId": "00000000-0000-0000-0000-000000000000",
                "payload": {}
            }]
        """.formatted(UUID.randomUUID());

        given()
                .contentType("application/json")
                .body(command)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("FAILED"));
    }
}
