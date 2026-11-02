package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 09, tasks 2-3: ONE-TO-MANY reviews of a product, in two kinds (inheritance). */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class ReviewsTest {

    private static final String CUSTOMER = "{\"type\":\"CUSTOMER\",\"rating\":5,\"comment\":\"Great\",\"verifiedPurchase\":true}";
    private static final String CRITIC = "{\"type\":\"CRITIC\",\"rating\":3,\"comment\":\"Fine\",\"outlet\":\"Daily News\"}";

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private static void review(int productId, String json) {
        given().contentType(ContentType.JSON).body(json)
                .when().post("/products/" + productId + "/reviews")
                .then().statusCode(201);
    }

    @Test
    void aProductStartsWithoutReviews() {
        given().when().get("/products/1/reviews").then().statusCode(200).body("$", hasSize(0));
        given().when().get("/products/1/review-summary").then().statusCode(200)
                .body("count", equalTo(0)).body("average", equalTo(0.0f));
    }

    @Test
    void bothKindsAreStoredAndComeBackWithTheirOwnFields() {
        given().contentType(ContentType.JSON).body(CUSTOMER)
                .when().post("/products/1/reviews")
                .then().statusCode(201)
                .body("type", equalTo("CUSTOMER")).body("verifiedPurchase", equalTo(true))
                .body("$", not(hasKey("outlet"))).body("$", not(hasKey("product")));
        given().contentType(ContentType.JSON).body(CRITIC)
                .when().post("/products/1/reviews")
                .then().statusCode(201)
                .body("type", equalTo("CRITIC")).body("outlet", equalTo("Daily News"))
                .body("$", not(hasKey("verifiedPurchase")));
    }

    @Test
    void bothKindsShareOneTableToldApartByTheKindColumn() {
        review(1, CUSTOMER);
        review(1, CRITIC);
        assertThat(data.count("review"), equalTo(2L));
        assertThat(data.reviewKinds(), contains("CRITIC", "CUSTOMER"));
    }

    @Test
    void oneQueryReturnsBothKindsNewestFirst() {
        review(1, CUSTOMER);
        review(1, CRITIC);
        review(2, CUSTOMER);
        given().when().get("/products/1/reviews")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("type", contains("CRITIC", "CUSTOMER"));
    }

    @Test
    void theSummaryCountsAndAveragesBothKindsOfOneProduct() {
        review(1, CUSTOMER); // 5
        review(1, CRITIC); // 3
        review(1, "{\"type\":\"CUSTOMER\",\"rating\":4,\"comment\":\"Good\"}"); // 4
        review(2, "{\"type\":\"CUSTOMER\",\"rating\":1,\"comment\":\"Bad\"}"); // another product
        given().when().get("/products/1/review-summary")
                .then().statusCode(200).body("count", equalTo(3)).body("average", equalTo(4.0f));
        given().when().get("/products/2/review-summary")
                .then().body("count", equalTo(1)).body("average", equalTo(1.0f));
    }

    @Test
    void invalidReviewsAreRejected() {
        String[] bodies = {
                "{\"type\":\"CUSTOMER\",\"rating\":0,\"comment\":\"x\"}",
                "{\"type\":\"CUSTOMER\",\"rating\":6,\"comment\":\"x\"}",
                "{\"type\":\"CUSTOMER\",\"rating\":3,\"comment\":\"\"}",
                "{\"type\":\"CRITIC\",\"rating\":3,\"comment\":\"x\"}", // a critic must name the outlet
                "{\"type\":\"CRITIC\",\"rating\":3,\"comment\":\"x\",\"outlet\":\" \"}",
        };
        for (String body : bodies) {
            given().contentType(ContentType.JSON).body(body)
                    .when().post("/products/1/reviews")
                    .then().statusCode(400).body("code", equalTo("VALIDATION_ERROR"));
        }
        for (String body : new String[] { "{\"type\":\"ROBOT\",\"rating\":3,\"comment\":\"x\"}",
                "{\"rating\":3,\"comment\":\"x\"}" }) { // unknown or missing type
            given().contentType(ContentType.JSON).body(body)
                    .when().post("/products/1/reviews")
                    .then().statusCode(400).body("code", equalTo("MALFORMED_REQUEST"));
        }
        assertThat(data.count("review"), equalTo(0L));
    }

    @Test
    void reviewsOfAnUnknownProductIs404() {
        review(1, CUSTOMER); // the routes work for a known product...
        given().when().get("/products/1/review-summary").then().statusCode(200);
        given().contentType(ContentType.JSON).body(CUSTOMER).when().post("/products/999/reviews")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        given().when().get("/products/999/reviews").then().statusCode(404);
        given().when().get("/products/999/review-summary").then().statusCode(404);
    }

    @Test
    void deletingAProductDeletesItsReviewsAndLabelLinksToo() {
        review(1, CUSTOMER);
        review(1, CRITIC);
        review(2, CUSTOMER);
        given().contentType(ContentType.JSON).body("[\"sale\"]").put("/products/1/labels").then().statusCode(200);
        given().when().delete("/products/1").then().statusCode(204);
        assertThat(data.count("review"), equalTo(1L));
        assertThat(data.count("product_label"), equalTo(0L));
        given().when().get("/products/2/reviews").then().body("$", hasSize(1));
    }
}
