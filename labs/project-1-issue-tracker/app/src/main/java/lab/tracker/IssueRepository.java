package lab.tracker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

/**
 * Database access for issues. The list query is built from FIXED fragments: user input only ever travels as a
 * named parameter, never as part of the query text (lab 10), and the sort comes from a whitelist.
 */
@ApplicationScoped
public class IssueRepository implements PanacheRepository<Issue> {

    private static final String PRIORITY_RANK = "(case i.priority when lab.tracker.Priority.LOW then 0 "
            + "when lab.tracker.Priority.MEDIUM then 1 when lab.tracker.Priority.HIGH then 2 else 3 end)";

    /** Sort keys the API accepts, each mapped to a fixed ORDER BY fragment. {@code id} breaks ties: stable pages. */
    private static final Map<String, String> SORTS = Map.of(
            "created", "i.createdAt asc, i.id asc",
            "-created", "i.createdAt desc, i.id desc",
            "updated", "i.updatedAt asc, i.id asc",
            "-updated", "i.updatedAt desc, i.id desc",
            // priority is stored as text ('CRITICAL' < 'HIGH' < 'LOW' < 'MEDIUM' alphabetically): sort by importance
            "priority", PRIORITY_RANK + " asc, i.id asc",
            "-priority", PRIORITY_RANK + " desc, i.id desc");

    public static boolean isKnownSort(String sort) {
        return SORTS.containsKey(sort);
    }

    public record IssuePage(List<Issue> items, long total) {
    }

    public Optional<Issue> findByKey(IssueKey key) {
        return find("from Issue i join fetch i.project p where p.code = ?1 and i.number = ?2", key.code(), key.number())
                .firstResultOptional();
    }

    public IssuePage page(IssueFilter f, String sort, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (f.project() != null) {
            where.append(" and p.code = :project");
            params.put("project", f.project());
        }
        if (f.status() != null) {
            where.append(" and i.status = :status");
            params.put("status", f.status());
        }
        if (f.assignee() != null) {
            where.append(" and i.assignee = :assignee");
            params.put("assignee", f.assignee());
        }
        if (f.priority() != null) {
            where.append(" and i.priority = :priority");
            params.put("priority", f.priority());
        }
        if (f.label() != null) {
            where.append(" and :label member of i.labels");
            params.put("label", f.label());
        }
        if (f.text() != null) {
            // locate() is a plain substring search: unlike LIKE, a typed "%" or "_" means itself
            where.append(" and (locate(lower(:text), lower(i.title)) > 0"
                    + " or locate(lower(:text), lower(coalesce(i.description, ''))) > 0)");
            params.put("text", f.text());
        }

        // The count cannot use "join fetch"; the page can, because the join is many-to-one (it cannot multiply rows).
        TypedQuery<Long> count = getEntityManager()
                .createQuery("select count(i) from Issue i join i.project p" + where, Long.class);
        TypedQuery<Issue> items = getEntityManager()
                .createQuery("select i from Issue i join fetch i.project p" + where + " order by " + SORTS.get(sort),
                        Issue.class);
        params.forEach((name, value) -> {
            count.setParameter(name, value);
            items.setParameter(name, value);
        });
        items.setFirstResult(page * size).setMaxResults(size);
        return new IssuePage(items.getResultList(), count.getSingleResult());
    }

    public Map<Status, Long> countByStatus(Project project) {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : rows("select i.status, count(i) from Issue i where i.project = ?1 group by i.status", project)) {
            counts.put((Status) row[0], (Long) row[1]);
        }
        return counts;
    }

    /** Work still to do: everything that is not CLOSED. */
    public Map<Priority, Long> countOpenWorkByPriority(Project project) {
        Map<Priority, Long> counts = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            counts.put(p, 0L);
        }
        for (Object[] row : rows("select i.priority, count(i) from Issue i where i.project = ?1 and i.status <> ?2 "
                + "group by i.priority", project, Status.CLOSED)) {
            counts.put((Priority) row[0], (Long) row[1]);
        }
        return counts;
    }

    private List<Object[]> rows(String hql, Object... params) {
        TypedQuery<Object[]> q = getEntityManager().createQuery(hql, Object[].class);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return new ArrayList<>(q.getResultList());
    }
}
