package lab.tracker;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "issue")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    public Project project;

    @Column(nullable = false)
    public int number;

    @Column(nullable = false, length = 120)
    public String title;

    @Column(length = 2000)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    public Status status = Status.OPEN;

    @Column(length = 40)
    public String assignee;

    /**
     * LAZY by default; {@code @BatchSize} loads the labels of up to 50 issues of a page with ONE query instead of
     * one query per issue (the N+1 of lab 09, solved without turning the page query into a join fetch, which
     * cannot be paged in the database).
     */
    @ElementCollection
    @CollectionTable(name = "issue_label", joinColumns = @JoinColumn(name = "issue_id"))
    @Column(name = "label", nullable = false, length = 20)
    @BatchSize(size = 50)
    public Set<String> labels = new HashSet<>();

    /** The optimistic lock AND the source of the ETag: every change that reaches the row increments it. */
    @Version
    public long version;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** {@code @PreUpdate} runs only when something really changed, so a no-op PUT does not touch this. */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public IssueKey key() {
        return new IssueKey(project.code, number);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
