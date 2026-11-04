package lab.clinic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * The schema on its own, with raw SQL and no API: the answer to "can two appointments overlap?" must hold for
 * anybody who reaches the database. Every refusal is checked by NAME, so a test cannot pass because of another
 * constraint.
 */
@QuarkusTest
class SchemaTest {

    @Inject
    TestData data;

    @BeforeEach
    void fixture() {
        data.reset();
        data.execute("INSERT INTO practitioner (name, created_at) VALUES ('Dr. Ana', now()), ('Dr. Ben', now())");
        data.execute("INSERT INTO patient (full_name, email, created_at) VALUES ('Bea', 'bea@example.com', now()), ('Carl', 'carl@example.com', now())");
        data.execute("INSERT INTO treatment (name, duration_minutes) VALUES ('Checkup', 30)");
    }

    private static String appointment(int practitioner, int patient, String start, String end, String status) {
        return "INSERT INTO appointment (practitioner_id, patient_id, treatment_id, starts_at, ends_at, status, version, created_at, updated_at) "
                + "VALUES (" + practitioner + ", " + patient + ", 1, '" + start + "', '" + end + "', '" + status + "', 0, now(), now())";
    }

    private void refused(String sql, String constraintName) {
        Exception e = assertThrows(Exception.class, () -> data.execute(sql));
        String all = messages(e);
        assertTrue(all.contains(constraintName), "expected " + constraintName + " but got: " + all);
    }

    private static String messages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (; t != null; t = t.getCause()) {
            sb.append(t.getMessage()).append(" | ");
        }
        return sb.toString();
    }

    @Test
    void twoBookedAppointmentsOfOnePractitionerCannotOverlap() {
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED"));

        refused(appointment(1, 2, "2026-03-09 09:15:00+00", "2026-03-09 09:45:00+00", "BOOKED"),
                "appointment_no_overlap_practitioner");
    }

    @Test
    void anAppointmentThatStartsWhenAnotherEndsIsNotAnOverlap() {
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED"));

        assertDoesNotThrow(() -> data.execute(appointment(1, 2, "2026-03-09 09:30:00+00", "2026-03-09 10:00:00+00", "BOOKED")));
    }

    @Test
    void anotherPractitionerMayBeBookedAtTheSameTime() {
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED"));

        assertDoesNotThrow(() -> data.execute(appointment(2, 2, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED")));
    }

    @Test
    void aCancelledAppointmentDoesNotBlockItsTime() {
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "CANCELLED"));

        assertDoesNotThrow(() -> data.execute(appointment(1, 2, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED")));
    }

    @Test
    void aPatientCannotBeInTwoPlacesAtOnce() {
        data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "BOOKED"));

        refused(appointment(2, 1, "2026-03-09 09:10:00+00", "2026-03-09 09:40:00+00", "BOOKED"),
                "appointment_no_overlap_patient");
    }

    @Test
    void anAppointmentMustEndAfterItStarts() {
        Exception e = assertThrows(Exception.class,
                () -> data.execute(appointment(1, 1, "2026-03-09 09:30:00+00", "2026-03-09 09:30:00+00", "BOOKED")));
        assertTrue(messages(e).contains("appointment_check") || messages(e).contains("check constraint"), messages(e));
    }

    @Test
    void theStatusIsOneOfTheFour() {
        Exception e = assertThrows(Exception.class,
                () -> data.execute(appointment(1, 1, "2026-03-09 09:00:00+00", "2026-03-09 09:30:00+00", "PENDING")));
        assertTrue(messages(e).contains("appointment_status_check"), messages(e));
    }

    @Test
    void twoBlocksOfOneDayCannotOverlapButTouchingBlocksAndOtherDaysAreFine() {
        data.execute("INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time) VALUES (1, 1, '09:00', '13:00')");

        refused("INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time) VALUES (1, 1, '12:00', '14:00')",
                "working_hours_no_overlap");
        assertDoesNotThrow(() -> data.execute(
                "INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time) VALUES (1, 1, '13:00', '14:00')"));
        assertDoesNotThrow(() -> data.execute(
                "INSERT INTO working_hours (practitioner_id, weekday, start_time, end_time) VALUES (1, 2, '09:00', '13:00')"));
    }

    @Test
    void anEmailIsUniqueWhateverItsCase() {
        refused("INSERT INTO patient (full_name, email, created_at) VALUES ('Other Bea', 'BEA@Example.com', now())",
                "patient_email_lower_uq");
    }

    @Test
    void aTreatmentIsAWholeNumberOfQuarterHours() {
        Exception e = assertThrows(Exception.class,
                () -> data.execute("INSERT INTO treatment (name, duration_minutes) VALUES ('Odd', 20)"));
        assertTrue(messages(e).contains("treatment_duration_minutes_check"), messages(e));
    }
}
