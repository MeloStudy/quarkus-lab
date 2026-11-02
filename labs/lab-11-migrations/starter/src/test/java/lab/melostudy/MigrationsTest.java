package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/** Task 1: Flyway owns the schema in every profile and Hibernate only validates it. */
@QuarkusTest
class MigrationsTest {

    @Inject
    EntityManager em;

    @ConfigProperty(name = "quarkus.hibernate-orm.schema-management.strategy")
    String strategy;

    @Test
    void hibernateValidatesInsteadOfGeneratingTheSchema() {
        assertEquals("validate", strategy);
    }

    @Test
    void theMigrationsRanInOrderAndSucceeded() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank")
                .getResultList();
        assertTrue(rows.size() >= 3, "expected at least V1, V2 and V3, got " + rows.size());
        assertEquals("1", rows.get(0)[0]);
        assertEquals("2", rows.get(1)[0]);
        assertEquals("3", rows.get(2)[0]);
        assertTrue(rows.stream().allMatch(r -> (Boolean) r[1]), "every migration must have succeeded");
    }
}
