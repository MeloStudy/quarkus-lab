package lab.tracker;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommentRepository implements PanacheRepository<Comment> {

    public List<Comment> ofIssue(Issue issue) {
        return list("issue = ?1 order by id", issue);
    }
}
