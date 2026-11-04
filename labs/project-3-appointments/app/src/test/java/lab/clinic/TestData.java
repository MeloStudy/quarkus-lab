package lab.clinic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Test-only helper: puts the database in a known state and runs raw SQL. Plain SQL on purpose, so the tests do
 * not depend on the code under study. Lives in src/test, so it never ships.
 */
@ApplicationScoped
public class TestData {

    @Inject
    EntityManager em;

    @Transactional
    public void reset() {
        em.createNativeQuery("TRUNCATE TABLE appointment, working_hours, patient, treatment, practitioner RESTART IDENTITY CASCADE")
                .executeUpdate();
    }

    @Transactional
    public long count(String table) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult()).longValue();
    }

    /** Raw SQL that is expected to be refused by the database (a constraint). Throws when it is. */
    @Transactional
    public void execute(String sql) {
        em.createNativeQuery(sql).executeUpdate();
    }
}
