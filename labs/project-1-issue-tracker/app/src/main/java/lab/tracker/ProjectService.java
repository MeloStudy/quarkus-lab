package lab.tracker;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProjectService {

    private final ProjectRepository projects;
    private final IssueRepository issues;

    ProjectService(ProjectRepository projects, IssueRepository issues) {
        this.projects = projects;
        this.issues = issues;
    }

    /**
     * Uniqueness is a DATABASE rule (UNIQUE on code): we try to insert and translate the violation. A "check if it
     * exists, then insert" would still fail for two simultaneous requests (lab 08).
     */
    @Transactional
    public ProjectResponse create(ProjectRequest in) {
        Project p = new Project();
        p.code = in.code();
        p.name = in.name().strip();
        try {
            projects.persistAndFlush(p);
        } catch (PersistenceException e) {
            if (causedByConstraint(e)) {
                throw TrackerException.conflict("DUPLICATE_PROJECT", "Project " + in.code() + " already exists");
            }
            throw e;
        }
        return ProjectResponse.from(p);
    }

    @Transactional
    public List<ProjectResponse> list() {
        return projects.allByCode().stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse get(String code) {
        return ProjectResponse.from(require(code));
    }

    @Transactional
    public SummaryResponse summary(String code) {
        Project p = require(code);
        var byStatus = issues.countByStatus(p);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new SummaryResponse(p.code, total, byStatus, issues.countOpenWorkByPriority(p));
    }

    private Project require(String code) {
        return projects.findByCode(code)
                .orElseThrow(() -> TrackerException.notFound("Project " + code + " does not exist"));
    }

    private static boolean causedByConstraint(Throwable t) {
        for (; t != null; t = t.getCause()) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}
