package lab.clinic;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reminder")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id")
    public Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "due_at", nullable = false)
    public Instant dueAt;

    /** When it may be tried next; also the lease that keeps two instances from sending the same reminder. */
    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(nullable = false)
    public int attempts;

    @Column(name = "last_error", length = 200)
    public String lastError;

    @Column(name = "sent_at")
    public Instant sentAt;
}
