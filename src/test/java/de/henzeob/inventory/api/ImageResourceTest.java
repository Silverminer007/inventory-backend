package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ImageResourceTest {

    @Test
    public void testGetImagesForNonExistentItem() {
        given()
                .when().get("/api/v1/items/99999/images")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetImagesForNonExistentContainer() {
        given()
                .when().get("/api/v1/containers/99999/images")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetNonExistentImage() {
        given()
                .when().get("/api/v1/images/99999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testDeleteNonExistentImage() {
        given()
                .when().delete("/api/v1/images/99999")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUploadImageForNonExistentItem() {
        given()
                .multiPart("file", "test.jpg", "fake image content".getBytes(), "image/jpeg")
                .when().post("/api/v1/items/99999/images")
                .then()
                .statusCode(404);
    }

    @Test
    public void testUploadImageForNonExistentContainer() {
        given()
                .multiPart("file", "test.jpg", "fake image content".getBytes(), "image/jpeg")
                .when().post("/api/v1/containers/99999/images")
                .then()
                .statusCode(404);
    }
}
