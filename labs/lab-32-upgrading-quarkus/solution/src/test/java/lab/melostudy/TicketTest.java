package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** The behavior the service has always had. These tests passed before the upgrade and must pass after it. */
@QuarkusTest
class TicketTest {

    @Test
    void aTicketIsReturned() {
        given().get("/tickets/t1").then().statusCode(200).body("id", is("t1")).body("title", is("Printer on fire"));
    }

    @Test
    void anUnknownTicketIsNotFound() {
        given().get("/tickets/t1").then().statusCode(200);

        given().get("/tickets/nope").then().statusCode(404);
    }
}
