package lab.tracker;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 5)
    public String code;

    @Column(nullable = false, length = 80)
    public String name;

    @Column(name = "next_number", nullable = false)
    public int nextNumber = 1;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** Hands out the next issue number. Callers must hold the row lock (see ProjectRepository.lockByCode). */
    public int takeNextNumber() {
        return nextNumber++;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
