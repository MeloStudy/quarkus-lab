package lab.tracker;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventRepository implements PanacheRepository<IssueEvent> {

    public List<IssueEvent> ofIssue(Issue issue) {
        return list("issue = ?1 order by id", issue);
    }
}
