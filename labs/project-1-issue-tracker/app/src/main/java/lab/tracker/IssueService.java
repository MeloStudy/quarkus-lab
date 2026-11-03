package lab.tracker;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * The business rules of an issue. Every public method is ONE transaction: the change, its audit event and the
 * response are built together, so either all of it happens or none of it does.
 * <p>Order of checks, always the same: the thing exists (404) -> the client saw its latest version (428/412) ->
 * the state of the world allows it (409). A stale client must learn "reload" before it learns anything else.
 */
@ApplicationScoped
public class IssueService {

    public record IssueList(List<IssueResponse> items, long total) {
    }

    private final ProjectRepository projects;
    private final IssueRepository issues;
    private final CommentRepository comments;
    private final EventRepository events;

    IssueService(ProjectRepository projects, IssueRepository issues, CommentRepository comments,
            EventRepository events) {
        this.projects = projects;
        this.issues = issues;
        this.comments = comments;
        this.events = events;
    }

    @Transactional
    public IssueResponse create(String projectCode, CreateIssueRequest in) {
        // Locks the project row until the commit: concurrent creators queue here and get consecutive numbers.
        Project project = projects.lockByCode(projectCode)
                .orElseThrow(() -> TrackerException.notFound("Project " + projectCode + " does not exist"));
        Issue issue = new Issue();
        issue.project = project;
        issue.number = project.takeNextNumber();
        issue.title = in.title().strip();
        issue.description = in.description();
        issue.type = in.type();
        issue.priority = in.priority() == null ? Priority.MEDIUM : in.priority();
        issue.labels.addAll(in.labels() == null ? Set.of() : in.labels());
        issues.persist(issue);
        record(issue, EventKind.CREATED, "Created as " + issue.type + " with priority " + issue.priority);
        return IssueResponse.from(issue);
    }

    @Transactional
    public IssueResponse get(String key) {
        return IssueResponse.from(require(key));
    }

    @Transactional
    public IssueResponse update(String key, Long expectedVersion, UpdateIssueRequest in) {
        Issue issue = require(key);
        checkVersion(issue, expectedVersion);
        if (issue.status == Status.CLOSED) {
            throw TrackerException.conflict("ISSUE_CLOSED", "A closed issue cannot be changed");
        }
        if (issue.status == Status.IN_PROGRESS && in.assignee() == null) {
            throw TrackerException.conflict("ASSIGNEE_REQUIRED", "An issue in progress needs an assignee");
        }

        if (!Objects.equals(issue.assignee, in.assignee())) {
            record(issue, EventKind.ASSIGNEE_CHANGED, name(issue.assignee) + " -> " + name(in.assignee()));
            issue.assignee = in.assignee();
        }
        if (issue.priority != in.priority()) {
            record(issue, EventKind.PRIORITY_CHANGED, issue.priority + " -> " + in.priority());
            issue.priority = in.priority();
        }
        issue.title = in.title().strip();
        issue.description = in.description();
        Set<String> labels = in.labels() == null ? Set.of() : in.labels();
        if (!issue.labels.equals(labels)) {
            // Touching the collection marks the issue as changed (version + 1) even if the content were the same.
            issue.labels.clear();
            issue.labels.addAll(labels);
        }
        return flushed(issue);
    }

    @Transactional
    public IssueResponse transition(String key, Long expectedVersion, Status to) {
        Issue issue = require(key);
        checkVersion(issue, expectedVersion);
        if (!issue.status.canMoveTo(to)) {
            throw TrackerException.conflict("INVALID_TRANSITION", "An issue cannot go from " + issue.status + " to " + to);
        }
        if (to == Status.IN_PROGRESS && issue.assignee == null) {
            throw TrackerException.conflict("ASSIGNEE_REQUIRED", "An issue needs an assignee before it is started");
        }
        record(issue, EventKind.STATUS_CHANGED, issue.status + " -> " + to);
        issue.status = to;
        return flushed(issue);
    }

    @Transactional
    public IssueList page(IssueFilter filter, String sort, int page, int size) {
        IssueRepository.IssuePage result = issues.page(filter, sort, page, size);
        return new IssueList(result.items().stream().map(IssueResponse::from).toList(), result.total());
    }

    @Transactional
    public CommentResponse addComment(String key, CommentRequest in) {
        Issue issue = require(key);
        if (issue.status == Status.CLOSED) {
            throw TrackerException.conflict("ISSUE_CLOSED", "A closed issue cannot be commented");
        }
        Comment c = new Comment();
        c.issue = issue;
        c.author = in.author().strip();
        c.body = in.body().strip();
        comments.persist(c);
        return CommentResponse.from(c);
    }

    @Transactional
    public List<CommentResponse> comments(String key) {
        return comments.ofIssue(require(key)).stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public List<EventResponse> history(String key) {
        return events.ofIssue(require(key)).stream().map(EventResponse::from).toList();
    }

    // ---- helpers

    private Issue require(String key) {
        IssueKey parsed = IssueKey.parse(key);
        return issues.findByKey(parsed)
                .orElseThrow(() -> TrackerException.notFound("Issue " + parsed + " does not exist"));
    }

    /** No header at all: 428. A header that is not the current version (or is garbage): 412. */
    private static void checkVersion(Issue issue, Long expectedVersion) {
        if (expectedVersion == null) {
            throw TrackerException.preconditionRequired();
        }
        if (expectedVersion != issue.version) {
            throw TrackerException.preconditionFailed("The issue changed since you read it; reload it and try again");
        }
    }

    private void record(Issue issue, EventKind kind, String detail) {
        events.persist(IssueEvent.of(issue, kind, detail));
    }

    /**
     * Sends the UPDATE now, inside the method: if somebody else changed the row after we read it, the
     * {@code OptimisticLockException} is thrown here (mapped to 412) instead of at the commit, where it would arrive
     * wrapped in a transaction exception. It also gives us the new {@code version} for the response.
     */
    private IssueResponse flushed(Issue issue) {
        issues.flush();
        return IssueResponse.from(issue);
    }

    private static String name(String assignee) {
        return assignee == null ? "unassigned" : assignee;
    }
}
