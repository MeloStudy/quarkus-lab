package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Test-only helper: puts the database in a known state (the five sample products with ids 1-5,
 * no price history). Plain SQL on purpose, so it does not depend on the code under study.
 * Lives in src/test, so it never ships in the application.
 */
@ApplicationScoped
public class TestData {

    @Inject
    EntityManager em;

    @Transactional
    public void reset() {
        em.createNativeQuery("TRUNCATE TABLE product, price_change RESTART IDENTITY").executeUpdate();
        insert("Clean Code", "books", "30.50");
        insert("Effective Java", "books", "45.00");
        insert("Domain-Driven Design", "books", "50.25");
        insert("Chess", "games", "20.00");
        insert("Catan", "games", "40.50");
    }

    private void insert(String name, String category, String price) {
        em.createNativeQuery("INSERT INTO product (name, name_key, category, price) VALUES (?1, lower(?1), ?2, CAST(?3 AS numeric))")
                .setParameter(1, name).setParameter(2, category).setParameter(3, price)
                .executeUpdate();
    }
}
