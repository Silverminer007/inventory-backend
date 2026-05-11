package de.henzeob.inventory.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CategoryResourceTest {

    @Test
    void getAllCategories_includesSeededDefault() {
        given()
                .when().get("/api/v1/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].id", notNullValue());
    }

    @Test
    void searchByName_matchesSeededCategory() {
        given()
                .queryParam("q", "Sonstiges")
                .when().get("/api/v1/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].name", is("Sonstiges"));
    }

    @Test
    void searchByName_noMatch_returnsEmptyList() {
        given()
                .queryParam("q", "NonExistentCategoryXyz987")
                .when().get("/api/v1/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void getByShortCode_found() {
        given()
                .when().get("/api/v1/categories/by-short-code/XX")
                .then()
                .statusCode(200)
                .body("name", is("Sonstiges"))
                .body("shortCode", is("XX"))
                .body("id", notNullValue());
    }

    @Test
    void getByShortCode_notFound_returns404() {
        given()
                .when().get("/api/v1/categories/by-short-code/NONEXISTENT")
                .then()
                .statusCode(404);
    }

    @Test
    void getById_seededCategory_found() {
        String id = given().get("/api/v1/categories/by-short-code/XX")
                .then().statusCode(200)
                .extract().jsonPath().getString("id");

        given()
                .when().get("/api/v1/categories/" + id)
                .then()
                .statusCode(200)
                .body("shortCode", is("XX"))
                .body("name", is("Sonstiges"));
    }

    @Test
    void getById_notFound_returns404() {
        given()
                .when().get("/api/v1/categories/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }
}