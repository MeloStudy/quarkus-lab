package lab.melostudy;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ScopesTest {

    @Inject
    HitWriter writer;

    @Inject
    HitReader reader;

    @Inject
    HitCounter counter;

    @Test
    void writerAndReaderShareTheSameCounter() {
        int before = reader.hits();
        writer.hit();
        writer.hit();
        assertEquals(before + 2, reader.hits());
    }

    @Test
    void sharedBeansAreInjectedAsClientProxies() {
        assertTrue(counter.getClass().getName().contains("ClientProxy"),
                "Se esperaba un client proxy y llegó: " + counter.getClass().getName());
    }

    @Test
    void everyHttpRequestGetsItsOwnRequestContext() {
        String first = get("/demo/request-id").asString();
        String second = get("/demo/request-id").asString();
        assertNotEquals(first, second);
    }
}
