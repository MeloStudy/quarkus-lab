package lab.melostudy;

import org.hibernate.SessionFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

/**
 * Test-only helper: counts the SQL statements Hibernate prepares, using its statistics
 * ({@code quarkus.hibernate-orm.statistics=true} in the test profile).
 */
@ApplicationScoped
public class QueryCounter {

    @Inject
    EntityManagerFactory emf;

    public void reset() {
        emf.unwrap(SessionFactory.class).getStatistics().clear();
    }

    public long statements() {
        return emf.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
    }
}
