package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** The same ideas through HTTP: the three demo sources, an audit trail and a stream of readings. */
@QuarkusTest
class QuoteResourceTest {

    @Inject
    DemoSources sources;

    @Inject
    AuditTrail audit;

    @BeforeEach
    void clean() {
        sources.reset();
        audit.reset();
    }

    @Test
    void theCheapestSourceAnswers() {
        given().get("/quotes/A-1").then().statusCode(200).body("source", is("alpha"));
    }

    @Test
    void everyRequestIsRecordedInTheAuditTrail() {
        given().get("/quotes/A-1").then().statusCode(200);
        given().get("/quotes/A-2").then().statusCode(200);

        assertEquals(2, audit.count(), "the audit trail was not written: somebody never subscribed to it");
    }

    @Test
    void oneSourceDownStillGetsAQuote() {
        sources.behavior("alpha").failing = true;

        given().get("/quotes/A-1").then().statusCode(200).body("source", is("beta"));
    }

    @Test
    void aSlowSourceDoesNotHoldTheAnswerBack() {
        sources.behavior("alpha").delayMillis = 5000;

        given().get("/quotes/A-1").then().statusCode(200).body("source", is("beta")).time(lessThan(1500L));
    }

    @Test
    void noSourceNoQuote() {
        sources.behavior("alpha").failing = true;
        sources.behavior("beta").failing = true;
        sources.behavior("gamma").failing = true;

        given().get("/quotes/A-1").then().statusCode(503).body("code", is("NO_QUOTE"));
    }

    @Test
    void severalSkusAreQuotedTogether() {
        given().get("/quotes?skus=A-1&skus=A-2&skus=A-3").then().statusCode(200).body("$", hasSize(3)).body("source", everyItem(is("alpha")));
    }

    @Test
    void readingsAreAStreamOfAskedForItems() {
        given().get("/readings?count=5").then().statusCode(200).body("$", hasSize(5)).body("[0]", is(0)).body("[4]", is(4));
    }
}
