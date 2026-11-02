package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;

/**
 * The database layer on its own. {@code @TestTransaction} wraps every test in a transaction that is ROLLED BACK at the
 * end, so each test starts from the database it found and leaves it as it was.
 */
@QuarkusTest
@TestTransaction
class AccountRepositoryTest {

    @Inject
    AccountRepository accounts;

    @Test
    void anAccountIsFoundByItsCustomer() {
        accounts.persist(new Account("ana", 5));

        assertEquals(5, accounts.findByCustomer("ana").orElseThrow().points);
    }

    @Test
    void anUnknownCustomerHasNoAccount() {
        assertTrue(accounts.findByCustomer("nobody").isEmpty());
    }

    @Test
    void thereIsOneAccountPerCustomer() {
        accounts.persist(new Account("ana", 5));

        assertThrows(PersistenceException.class, () -> {
            accounts.persist(new Account("ana", 1));
            accounts.flush(); // the UNIQUE constraint is checked when the INSERT really runs
        });
    }

    @Test
    void oneTestCreatesTheCustomerShared() {
        accounts.persist(new Account("shared", 1));
        accounts.flush();
    }

    @Test
    void anotherTestCreatesTheSameCustomerAgain() {
        // If the first test had committed, this one would fail with a unique violation, depending on the order
        accounts.persist(new Account("shared", 1));
        accounts.flush();
    }

    @Test
    void everyTestStartsWithAnEmptyTable() {
        assertEquals(0, accounts.count());
        accounts.persist(new Account("someone", 1));
        accounts.flush();
        assertEquals(1, accounts.count());
    }

    @Test
    void whatTheTestWroteInsideItsTransactionIsInvisibleToHttpCalls() {
        accounts.persist(new Account("pending", 3));
        accounts.flush();

        assertEquals(1, accounts.count()); // the test sees its own uncommitted row...
        given().get("/rewards/pending").then().statusCode(404); // ...the server, in another transaction, does not
    }
}
