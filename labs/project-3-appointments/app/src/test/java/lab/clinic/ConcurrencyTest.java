package lab.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * The question of the project under pressure: twelve different patients ask for the same practitioner at the same
 * instant. There is no lock and no "check first" in the code, so the only judge is the exclusion constraint.
 */
@QuarkusTest
@TestSecurity(user = "front-desk", roles = "reception")
class ConcurrencyTest {

    private static final int CLIENTS = 12;

    @Inject
    TestData data;

    @BeforeEach
    void clean() {
        data.reset();
        Clinic.clockAt(Clinic.NOW);
    }

    @Test
    void exactlyOneOfManySimultaneousRequestsGetsTheSlot() throws Exception {
        long ana = Clinic.practitioner("Dr. Ana");
        Clinic.weekdayHours(ana);
        long checkup = Clinic.treatment("Checkup", 30);
        List<Long> patients = new ArrayList<>();
        for (int i = 0; i < CLIENTS; i++) {
            patients.add(Clinic.patient("Patient " + i, "p" + i + "@example.com"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
        CountDownLatch ready = new CountDownLatch(CLIENTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (long patient : patients) {
            statuses.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return Clinic.book(ana, patient, checkup, "2026-03-09T09:00:00Z").statusCode();
            }));
        }
        ready.await();
        go.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> status : statuses) {
            switch (status.get()) {
                case 201 -> created++;
                case 409 -> conflicts++;
                default -> throw new AssertionError("unexpected status " + status.get());
            }
        }
        pool.shutdown();

        assertEquals(1, created, "exactly one booking");
        assertEquals(CLIENTS - 1, conflicts, "everybody else is told the slot is taken");
        assertEquals(1, data.count("appointment"));
    }

    @Test
    void onePatientAskingForTwoPractitionersAtOnceGetsOnlyOne() throws Exception {
        long ana = Clinic.practitioner("Dr. Ana");
        long ben = Clinic.practitioner("Dr. Ben");
        Clinic.weekdayHours(ana);
        Clinic.weekdayHours(ben);
        long checkup = Clinic.treatment("Checkup", 30);
        long bea = Clinic.patient("Bea", "bea@example.com");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        Future<Integer> withAna = pool.submit(() -> {
            go.await();
            return Clinic.book(ana, bea, checkup, "2026-03-09T09:00:00Z").statusCode();
        });
        Future<Integer> withBen = pool.submit(() -> {
            go.await();
            return Clinic.book(ben, bea, checkup, "2026-03-09T09:00:00Z").statusCode();
        });
        go.countDown();
        List<Integer> results = List.of(withAna.get(), withBen.get());
        pool.shutdown();

        assertEquals(1, results.stream().filter(s -> s == 201).count(), results.toString());
        assertEquals(1, results.stream().filter(s -> s == 409).count(), results.toString());
        assertEquals(1, data.count("appointment"));
    }
}
