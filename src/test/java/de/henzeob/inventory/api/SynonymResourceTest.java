package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

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
        String requestBody = """
            {
                "canonicalTerm": "Fernseher",
                "synonym": "TV"
            }
        """;

        // Create synonym
        Long id = given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/synonyms")
                .then()
                .statusCode(201)
                .body("canonicalTerm", is("Fernseher"))
                .body("synonym", is("TV"))
                .body("id", notNullValue())
                .extract().jsonPath().getLong("id");

        // Verify it appears in the list
        given()
                .when().get("/api/v1/synonyms")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // Delete synonym
        given()
                .when().delete("/api/v1/synonyms/" + id)
                .then()
                .statusCode(204);
    }

    @Test
    public void testCreateSynonymValidation() {
        String requestBody = """
            {
                "canonicalTerm": "",
                "synonym": ""
            }
        """;

        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/api/v1/synonyms")
                .then()
                .statusCode(400);
    }
}
