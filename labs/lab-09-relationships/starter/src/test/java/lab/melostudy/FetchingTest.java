package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 09, task 4: listing products with their labels must not cost one query per product (the N+1 problem). */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class FetchingTest {

    @Inject
    TestData data;

    @Inject
    QueryCounter counter;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    @Test
    void listingFiveProductsWithLabelsRunsAHandfulOfStatements() {
        for (int id = 1; id <= 5; id++) {
            given().contentType(ContentType.JSON).body("[\"sale\",\"label-" + id + "\"]")
                    .put("/products/" + id + "/labels").then().statusCode(200);
        }
        counter.reset();
        given().queryParam("size", 10).when().get("/products")
                .then().statusCode(200).body("[0].labels", contains("label-1", "sale")); // the labels are really there
        // the page, its total, and the labels of all the products: with N+1 this would be 7
        assertThat(counter.statements(), lessThanOrEqualTo(4L));
    }
}
