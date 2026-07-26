package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.ROOT_ID;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.payload;

/** Direct binary upload/download endpoints backing ITEM_IMAGE_CREATE/CONTAINER_IMAGE_CREATE. */
@QuarkusTest
public class ImageUploadResourceTest {

    private byte[] pngBytes(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Smooth gradient (rather than noise) so PNG's deflate compresses it well,
                // keeping the raw upload under the 5MB cap while still exercising re-compression.
                int r = (x * 255) / Math.max(1, size);
                int g = (y * 255) / Math.max(1, size);
                image.setRGB(x, y, (r << 16) | (g << 8));
            }
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UUID createItemImage() {
        UUID head = currentHead();
        UUID itemId = UUID.randomUUID();
        head = applyOk(head, "ITEM_CREATE",
                payload("id", itemId.toString(), "name", "Photo Item", "container", ROOT_ID.toString(), "quantity", 1, "created_at", PAST));
        UUID imageId = UUID.randomUUID();
        applyOk(head, "ITEM_IMAGE_CREATE", payload("id", imageId.toString(), "item", itemId.toString(), "created_at", PAST));
        return imageId;
    }

    @Test
    public void uploadThenDownload_roundTrips() {
        UUID imageId = createItemImage();

        RestAssured.given()
                .multiPart("file", "photo.png", pngBytes(64), "image/png")
                .post("/api/v2/uploadImage/{id}", imageId)
                .then().statusCode(200);

        byte[] downloaded = RestAssured.given()
                .get("/api/v2/images/{id}", imageId)
                .then().statusCode(200)
                .contentType("image/jpeg")
                .extract().asByteArray();

        org.junit.jupiter.api.Assertions.assertTrue(downloaded.length > 0);
    }

    @Test
    public void upload_rejectsUnsupportedContentType() {
        UUID imageId = createItemImage();

        RestAssured.given()
                .multiPart("file", "notes.txt", "hello".getBytes(), "text/plain")
                .post("/api/v2/uploadImage/{id}", imageId)
                .then().statusCode(400);
    }

    @Test
    public void upload_rejectsOversizedFile() {
        UUID imageId = createItemImage();
        byte[] tooLarge = new byte[6 * 1024 * 1024];

        RestAssured.given()
                .multiPart("file", "huge.png", tooLarge, "image/png")
                .post("/api/v2/uploadImage/{id}", imageId)
                .then().statusCode(400);
    }

    @Test
    public void download_nonExistentImage_404() {
        RestAssured.given()
                .get("/api/v2/images/{id}", UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    public void compressesLargeImageBelowTargetSize() {
        UUID imageId = createItemImage();

        RestAssured.given()
                .multiPart("file", "big.png", pngBytes(2000), "image/png")
                .post("/api/v2/uploadImage/{id}", imageId)
                .then().statusCode(200);

        byte[] downloaded = RestAssured.given()
                .get("/api/v2/images/{id}", imageId)
                .then().statusCode(200)
                .extract().asByteArray();

        org.junit.jupiter.api.Assertions.assertTrue(downloaded.length <= 500 * 1024,
                "compressed image should be at most 500KB, was " + downloaded.length);
    }
}
