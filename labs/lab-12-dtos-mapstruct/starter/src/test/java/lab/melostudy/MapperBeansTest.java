package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Task 1 and 2: the mappers are generated at compile time and are CDI beans (so they can be injected).
 * Name them {@code ProductMapper} and {@code ReviewMapper}: MapStruct generates {@code <Name>Impl}.
 */
@QuarkusTest
class MapperBeansTest {

    @Test
    void mapStructGeneratedCdiBeansForProductsAndReviews() throws ClassNotFoundException {
        for (String mapper : new String[] { "lab.melostudy.ProductMapper", "lab.melostudy.ReviewMapper" }) {
            Class<?> generated = Class.forName(mapper + "Impl");
            assertTrue(generated.isAnnotationPresent(ApplicationScoped.class), generated + " is not @ApplicationScoped");
            assertNotNull(Arc.container().instance(Class.forName(mapper)).get(), mapper + " cannot be injected");
        }
    }
}
