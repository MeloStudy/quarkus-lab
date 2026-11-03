package lab.tracker;

import java.time.Instant;

import jakarta.persistence.Column;
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
import jakarta.persistence.Table;

/** One line of the audit trail of an issue. Written in the SAME transaction as the change it describes. */
@Entity
@Table(name = "issue_event")
public class IssueEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    public Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EventKind kind;

    @Column(nullable = false, length = 200)
    public String detail;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static IssueEvent of(Issue issue, EventKind kind, String detail) {
        IssueEvent e = new IssueEvent();
        e.issue = issue;
        e.kind = kind;
        e.detail = detail;
        return e;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
