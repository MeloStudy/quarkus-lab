package lab.tracker;

import org.hibernate.SessionFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

/** Test-only: counts the SQL statements Hibernate prepares ({@code quarkus.hibernate-orm.statistics=true} in %test). */
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
