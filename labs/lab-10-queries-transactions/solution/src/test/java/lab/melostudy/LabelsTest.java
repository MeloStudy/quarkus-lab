package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 09, task 1: MANY-TO-MANY between products and labels. */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class LabelsTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private static void setLabels(int productId, String json) {
        given().contentType(ContentType.JSON).body(json)
                .when().put("/products/" + productId + "/labels")
                .then().statusCode(200);
    }

    @Test
    void aProductStartsWithoutLabels() {
        given().when().get("/products/1").then().statusCode(200).body("labels", empty());
    }

    @Test
    void settingLabelsReplacesThePreviousOnesAndSortsThem() {
        setLabels(1, "[\"sale\",\"classic\"]");
        given().when().get("/products/1").then().body("labels", contains("classic", "sale"));
        setLabels(1, "[\"new\"]");
        given().when().get("/products/1").then().body("labels", contains("new"));
        setLabels(1, "[]");
        given().when().get("/products/1").then().body("labels", empty());
    }

    @Test
    void theSameLabelIsSharedNotDuplicated() {
        setLabels(1, "[\"sale\"]");
        setLabels(2, "[\"sale\"]");
        setLabels(3, "[\"sale\",\"sale\"]"); // repeated names collapse
        assertThat(data.count("label"), equalTo(1L));
        assertThat(data.count("product_label"), equalTo(3L));
    }

    @Test
    void productsCanBeFilteredByLabelTogetherWithCategoryAndPaging() {
        setLabels(1, "[\"sale\"]");
        setLabels(2, "[\"sale\"]");
        setLabels(4, "[\"sale\"]");
        given().queryParam("label", "sale").when().get("/products")
                .then().statusCode(200).header("X-Total-Count", "3").body("id", contains(1, 2, 4));
        given().queryParam("label", "sale").queryParam("category", "BOOKS").when().get("/products")
                .then().header("X-Total-Count", "2").body("id", contains(1, 2));
        given().queryParam("label", "sale").queryParam("size", 2).queryParam("page", 1).when().get("/products")
                .then().header("X-Total-Count", "3").body("id", contains(4));
        given().queryParam("label", "unknown").when().get("/products")
                .then().statusCode(200).header("X-Total-Count", "0").body("$", hasSize(0));
    }

    @Test
    void deletingAProductKeepsTheLabelForTheOthers() {
        setLabels(1, "[\"sale\"]");
        setLabels(2, "[\"sale\"]");
        given().when().delete("/products/1").then().statusCode(204);
        given().when().get("/products/2").then().body("labels", contains("sale"));
        assertThat(data.count("label"), equalTo(1L));
        assertThat(data.count("product_label"), equalTo(1L));
    }

    @Test
    void invalidLabelListsAreRejected() {
        for (String body : new String[] { "[\"Sale\"]", "[\"\"]", "[\"has space\"]",
                "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\"]" }) {
            given().contentType(ContentType.JSON).body(body)
                    .when().put("/products/1/labels")
                    .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
        }
        given().when().get("/products/1").then().body("labels", empty());
    }

    @Test
    void labelsOfAnUnknownProductIs404() {
        setLabels(1, "[]"); // the route works for a known product...
        given().contentType(ContentType.JSON).body("[\"sale\"]")
                .when().put("/products/999/labels")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        assertThat(data.count("label"), equalTo(0L)); // ...and a failed request creates nothing
    }
}
