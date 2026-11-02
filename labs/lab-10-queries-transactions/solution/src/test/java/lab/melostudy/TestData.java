package lab.melostudy;

import java.util.List;

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
        // Only the tables that exist: a starter whose relationships are not mapped yet has fewer of them.
        @SuppressWarnings("unchecked")
        List<String> tables = em.createNativeQuery("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = current_schema() "
                + "AND table_name IN ('product', 'price_change', 'label', 'product_label', 'review', 'customer_order', 'audit_entry')").getResultList();
        em.createNativeQuery("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY").executeUpdate();
        insert("Clean Code", "books", "30.50");
        insert("Effective Java", "books", "45.00");
        insert("Domain-Driven Design", "books", "50.25");
        insert("Chess", "games", "20.00");
        insert("Catan", "games", "40.50");
    }

    /** Audit outcomes in the order they were written. */
    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> auditOutcomes() {
        return em.createNativeQuery("SELECT outcome FROM audit_entry ORDER BY id").getResultList();
    }

    /** Puts exactly this many units in stock. */
    @Transactional
    public void setStock(long productId, int stock) {
        em.createNativeQuery("UPDATE product SET stock = ?1 WHERE id = ?2")
                .setParameter(1, stock).setParameter(2, productId).executeUpdate();
    }

    @Transactional
    public int stockOf(long productId) {
        return ((Number) em.createNativeQuery("SELECT stock FROM product WHERE id = ?1")
                .setParameter(1, productId).getSingleResult()).intValue();
    }

    /** Number of rows in a table (the name comes from our own tests, never from a request). */
    @Transactional
    public long count(String table) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult()).longValue();
    }

    /** The values of the {@code kind} column of the review table, sorted. */
    @SuppressWarnings("unchecked")
    @Transactional
    public List<String> reviewKinds() {
        return em.createNativeQuery("SELECT DISTINCT kind FROM review ORDER BY kind").getResultList();
    }

    private void insert(String name, String category, String price) {
        em.createNativeQuery("INSERT INTO product (name, name_key, category, price, stock) VALUES (?1, lower(?1), ?2, CAST(?3 AS numeric), 10)")
                .setParameter(1, name).setParameter(2, category).setParameter(3, price)
                .executeUpdate();
    }
}
