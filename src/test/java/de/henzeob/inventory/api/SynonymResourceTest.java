package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
public class SynonymResourceTest {

    @Test
    public void testGetAllSynonymsEndpoint() {
        given()
                .when().get("/api/v1/synonyms")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void testCreateAndDeleteSynonym() {
        // Create synonym via command
        String createCommand = """
            [{
                "commandId": "%s",
                "commandType": "SYNONYM_CREATE",
                "payload": {
                    "canonicalTerm": "Fernseher",
                    "synonym": "TV"
                }
            }]
        """.formatted(UUID.randomUUID());

        String id = given()
                .contentType("application/json")
                .body(createCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"))
                .body("[0].snapshot.canonicalTerm", is("Fernseher"))
                .body("[0].snapshot.synonym", is("TV"))
                .extract().jsonPath().getString("[0].entityId");

        // Verify it appears in the list
        given()
                .when().get("/api/v1/synonyms")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // Delete synonym via command
        String deleteCommand = """
            [{
                "commandId": "%s",
                "commandType": "SYNONYM_DELETE",
                "entityId": "%s",
                "payload": {}
            }]
        """.formatted(UUID.randomUUID(), id);

        given()
                .contentType("application/json")
                .body(deleteCommand)
                .when().post("/commands")
                .then()
                .statusCode(200)
                .body("[0].status", is("APPLIED"));
    }

    @Test
    public void testCreateSynonymValidation() {
        // Missing required fields — command should FAIL
        String command = """
            [{
                "commandId": "%s",
                "commandType": "SYNONYM_CREATE",
                "payload": {
                    "canonicalTerm": "",
                    "synonym": ""
                }
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
