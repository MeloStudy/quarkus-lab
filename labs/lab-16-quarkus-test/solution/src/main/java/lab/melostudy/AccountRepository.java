package lab.melostudy;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {

    public Optional<Account> findByCustomer(String customerId) {
        return find("customerId", customerId).firstResultOptional();
    }
}
