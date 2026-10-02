package lab.melostudy.repository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lab.melostudy.Artist;
import lab.melostudy.Customer;
import lab.melostudy.model.Publisher;
import lab.melostudy.model.cardinality.OrderLine;
import lab.melostudy.model.cardinality.PurchaseOrder;
import lab.melostudy.model.inheritance.Book;
import lab.melostudy.model.inheritance.Language;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PurchaseOrderRepositoryTest {
    @Inject
    CustomerRepository customerRepository;

    @Test
    @TestTransaction
    void shouldCreateAndFindAPurchaseOrder() {

        // Creates an Artist
        Artist artist = new Artist("artist name");

        // Creates a Customer
        Customer customer = new Customer("full name", "email");
        customerRepository.persist(customer);

        // Creates a Publisher
        Publisher publisher = new Publisher("publisher name");

        // Creates a Book
        Book book = new Book();
        book.title = "title";
        book.nbOfPages = 500;
        book.language = Language.ENGLISH;
        book.price = new BigDecimal(10);
        book.isbn = "isbn";
        // Sets the Publisher and Artist to the Book
        book.publisher = publisher;
        book.artist = artist;
        // Persists the Book with one Publisher and one Artist
        Book.persist(book);

        // Creates a PurchaseOrder with an OrderLine
        OrderLine orderLine = new OrderLine();
        orderLine.item = book;
        orderLine.quantity = 2;

        // creates a Purchase Order
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.customer = customer;
        purchaseOrder.addOrderLine(orderLine);

        // Persists the PurchaseOrder and one OrderLine
        PurchaseOrder.persist(purchaseOrder);
        assertNotNull(purchaseOrder.id);
    }
}