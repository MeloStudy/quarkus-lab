package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 3: the numbers an operator will want on a dashboard: how many, how long, how many waiting. */
@QuarkusTest
class MetricsTest extends OpsTestBase {

    private static void order(String type) {
        given().contentType("application/json").body("{\"sku\":\"A-100\",\"quantity\":1,\"type\":\"" + type + "\"}")
                .post("/api/orders").then().statusCode(201);
    }

    @Test
    void createdOrdersAreCountedPerType() {
        String before = Ops.metrics();
        order("standard");
        order("standard");
        order("express");
        String after = Ops.metrics();

        double standard = Ops.sample(after, "orders_created_total", ".*type=\"standard\".*") - Ops.sample(before, "orders_created_total", ".*type=\"standard\".*");
        double express = Ops.sample(after, "orders_created_total", ".*type=\"express\".*") - Ops.sample(before, "orders_created_total", ".*type=\"express\".*");
        assertThat(standard, equalTo(2.0));
        assertThat(express, equalTo(1.0));
    }

    @Test
    void rejectedOrdersAreNotCounted() {
        order("standard");
        String before = Ops.metrics();
        inventory.setAvailable(false);
        given().contentType("application/json").body("{\"sku\":\"A-100\",\"quantity\":1,\"type\":\"standard\"}")
                .post("/api/orders").then().statusCode(503);
        String after = Ops.metrics();
        assertThat(Ops.sample(after, "orders_created_total", ".*type=\"standard\".*"),
                equalTo(Ops.sample(before, "orders_created_total", ".*type=\"standard\".*")));
    }

    @Test
    void processingTimeIsMeasured() throws InterruptedException {
        double before = Ops.sample(Ops.metrics(), "orders_processing_seconds_count", ".*");
        order("standard");
        order("standard");
        double after = before;
        for (int i = 0; i < 40 && after - before < 2; i++) {
            Thread.sleep(50);
            after = Ops.sample(Ops.metrics(), "orders_processing_seconds_count", ".*");
        }
        assertThat(after - before, equalTo(2.0));
        assertThat(Ops.sample(Ops.metrics(), "orders_processing_seconds_sum", ".*"), greaterThan(0.0));
    }

    @Test
    void queueSizeIsAGaugeThatGoesUpAndDown() throws InterruptedException {
        processor.stall(true);
        Thread.sleep(200); // the worker may be in the middle of a 100 ms wait for the next order: let it notice the stall
        double before = Ops.sample(Ops.metrics(), "orders_queue_size", ".*");
        order("standard");
        order("standard");
        order("standard");
        assertThat(Ops.sample(Ops.metrics(), "orders_queue_size", ".*") - before, equalTo(3.0));

        processor.stall(false);
        double size = 3;
        for (int i = 0; i < 60 && size > 0; i++) {
            Thread.sleep(50);
            size = Ops.sample(Ops.metrics(), "orders_queue_size", ".*");
        }
        assertThat(size, equalTo(0.0));
    }
}
