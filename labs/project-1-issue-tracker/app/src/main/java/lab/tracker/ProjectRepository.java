package lab.tracker;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {

    public Optional<Project> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }

    /**
     * {@code SELECT ... FOR UPDATE}: whoever holds the row is the only one who can hand out issue numbers; the
     * others wait for the end of the transaction. Two requests can never read the same {@code nextNumber}.
     */
    public Optional<Project> lockByCode(String code) {
        return find("code", code).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    public List<Project> allByCode() {
        return listAll(Sort.by("code"));
    }
}
