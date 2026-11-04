package lab.clinic;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** The repositories that need nothing but the standard operations (lab 8), and the one for working hours. */
final class Repositories {

    private Repositories() {
    }

    @ApplicationScoped
    static class PractitionerRepository implements PanacheRepository<Practitioner> {
    }

    @ApplicationScoped
    static class TreatmentRepository implements PanacheRepository<Treatment> {
    }

    @ApplicationScoped
    static class PatientRepository implements PanacheRepository<Patient> {
    }

    @ApplicationScoped
    static class WorkingHoursRepository implements PanacheRepository<WorkingHours> {

        List<WorkingHours> ofPractitioner(long practitionerId) {
            return list("practitioner.id = ?1 order by weekday, startTime", practitionerId);
        }

        List<WorkingHours> ofPractitionerOnWeekday(long practitionerId, int weekday) {
            return list("practitioner.id = ?1 and weekday = ?2 order by startTime", practitionerId, (short) weekday);
        }

        void deleteAllOf(long practitionerId) {
            delete("practitioner.id = ?1", practitionerId);
        }
    }
}
