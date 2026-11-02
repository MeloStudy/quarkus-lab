package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Task 2: rules enforced by the database itself. The tests talk plain SQL, bypassing the application, the
 * way a script or another service would. They check the SQLSTATE (23505 unique violation, 23514 check
 * violation), not the name you gave to each constraint.
 */
@QuarkusTest
class ConstraintsTest {

    @Inject
    EntityManager em;

    @Inject
    TestData data;

    @BeforeEach
    void reset() {
        data.reset();
    }

    @Test
    void productNamesAreUniqueIgnoringCaseEvenWhenNameKeyIsBypassed() {
        // "Clean Code" exists. A writer that fills name_key with anything else still cannot repeat the name.
        assertEquals("23505", sqlStateOf("INSERT INTO product (name, name_key, category, price, stock) "
                + "VALUES ('CLEAN CODE', 'something-else', 'books', 10, 1)"));
    }

    @Test
    void aProductCannotCostZeroOrLess() {
        assertEquals("23514", sqlStateOf("UPDATE product SET price = 0 WHERE id = 1"));
        assertEquals("23514", sqlStateOf("UPDATE product SET price = -5 WHERE id = 1"));
    }

    @Test
    void stockCannotGoNegative() {
        assertEquals("23514", sqlStateOf("UPDATE product SET stock = -1 WHERE id = 1"));
    }

    @Test
    void aRatingMustBeBetweenOneAndFive() {
        assertEquals("23514", sqlStateOf(reviewWithRating(0)));
        assertEquals("23514", sqlStateOf(reviewWithRating(6)));
        // control: a valid rating goes in, so the failures above are about the rating and nothing else
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(reviewWithRating(5)).executeUpdate());
        assertEquals(1, data.count("review"));
    }

    private String reviewWithRating(int rating) {
        return "INSERT INTO review (product_id, kind, rating, comment, createdAt, verifiedPurchase) "
                + "VALUES (1, 'CUSTOMER', " + rating + ", 'ok', now(), true)";
    }

    /** Runs the statement in its own transaction and returns the SQLSTATE of the failure. */
    private String sqlStateOf(String sql) {
        Exception failure = assertThrows(Exception.class,
                () -> QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(sql).executeUpdate()),
                "the database must reject: " + sql);
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException sql1 && sql1.getSQLState() != null) {
                return sql1.getSQLState();
            }
            cause = cause.getCause();
        }
        throw new AssertionError("no SQLException in the cause chain", failure);
    }
}
