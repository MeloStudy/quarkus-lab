package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/** Lab 08, task 3: name uniqueness is enforced by the database, so it also holds under concurrency. */
@QuarkusTest
@QuarkusTestResource(RatesStubResource.class)
class UniqueNameTest {

    @Inject
    TestData data;

    @BeforeEach
    void resetData() {
        data.reset();
    }

    private static String product(String name) {
        return "{\"name\":\"" + name + "\",\"category\":\"books\",\"price\":10.00}";
    }

    private static int post(String name) {
        return given().contentType(ContentType.JSON).body(product(name)).post("/products").statusCode();
    }

    @Test
    void creatingADuplicateIsAConflictRegardlessOfCase() {
        given().contentType(ContentType.JSON).body(product("CLEAN CODE"))
                .when().post("/products")
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));
        // the failed insert left nothing behind
        given().when().get("/products").then().header("X-Total-Count", "5");
    }

    @Test
    void renamingToATakenNameIsAConflictButKeepingYourOwnNameIsFine() {
        given().contentType(ContentType.JSON).body(product("chess")).when().put("/products/1")
                .then().statusCode(409).body("code", equalTo("CONFLICT"));
        // product 4 is "Chess": saving it again with its own name (different case) must work
        given().contentType(ContentType.JSON).body(product("CHESS")).when().put("/products/4")
                .then().statusCode(200).body("name", equalTo("CHESS"));
    }

    @Test
    void aFailedUpdateChangesNothing() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"Catan\",\"category\":\"music\",\"price\":99.00}")
                .when().put("/products/1")
                .then().statusCode(409);
        given().when().get("/products/1").then()
                .body("name", equalTo("Clean Code"))
                .body("category", equalTo("books"))
                .body("price", equalTo(30.5f));
        given().when().get("/products/1/price-history").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void concurrentCreatesOfTheSameNameLetExactlyOneWin() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Integer> call = () -> {
                go.await();
                return post("Refactoring");
            };
            results.add(pool.submit(call));
        }
        go.countDown();
        int created = 0;
        int conflicts = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 201) {
                created++;
            } else if (status == 409) {
                conflicts++;
            }
        }
        pool.shutdown();
        assertThat("created", created, equalTo(1));
        assertThat("conflicts", conflicts, equalTo(threads - 1));
        given().queryParam("category", "books").when().get("/products").then().header("X-Total-Count", "4");
    }
}
