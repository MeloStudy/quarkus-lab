package lab.melostudy;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class CustomerRepositoryTest {

    @Inject
    CustomerRepository repository;

    @Test
    @TestTransaction
    void shouldCreateAndFindACustomer() {
        Customer customer = new Customer("full name", "email");

        repository.persist(customer);
        assertNotNull(customer.getId());

        customer = repository.findById(customer.getId());
        assertEquals("full name", customer.getFullName());
    }
}