package lab.clinic;

import java.time.Clock;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import lab.clinic.Repositories.PatientRepository;
import lab.clinic.Repositories.PractitionerRepository;
import lab.clinic.Repositories.TreatmentRepository;
import lab.clinic.Repositories.WorkingHoursRepository;
import lab.clinic.Requests.HoursBlock;
import lab.clinic.Requests.PatientRequest;
import lab.clinic.Requests.PractitionerRequest;
import lab.clinic.Requests.TreatmentRequest;
import lab.clinic.Responses.HoursView;
import lab.clinic.Responses.PatientView;
import lab.clinic.Responses.PractitionerView;
import lab.clinic.Responses.TreatmentView;

/** The reference data of the clinic: who works here, what is done, who the patients are, and when. */
@ApplicationScoped
public class CatalogService {

    private final PractitionerRepository practitioners;
    private final TreatmentRepository treatments;
    private final PatientRepository patients;
    private final WorkingHoursRepository hours;
    private final Clock clock;

    CatalogService(PractitionerRepository practitioners, TreatmentRepository treatments, PatientRepository patients,
            WorkingHoursRepository hours, Clock clock) {
        this.practitioners = practitioners;
        this.treatments = treatments;
        this.patients = patients;
        this.hours = hours;
        this.clock = clock;
    }

    @Transactional
    public PractitionerView createPractitioner(PractitionerRequest request) {
        Practitioner p = new Practitioner();
        p.name = request.name().strip();
        p.createdAt = clock.instant();
        practitioners.persist(p);
        return PractitionerView.of(p);
    }

    @Transactional
    public List<PractitionerView> practitioners() {
        return practitioners.list("order by name, id").stream().map(PractitionerView::of).toList();
    }

    @Transactional
    public PractitionerView practitioner(long id) {
        return PractitionerView.of(practitioners.findByIdOptional(id)
                .orElseThrow(() -> ClinicException.notFound("Practitioner " + id + " does not exist")));
    }

    @Transactional
    public TreatmentView createTreatment(TreatmentRequest request) {
        Treatment t = new Treatment();
        t.name = request.name().strip();
        t.durationMinutes = request.durationMinutes();
        if (t.durationMinutes % 15 != 0) {
            throw ClinicException.unprocessable("INVALID_DURATION", "A treatment lasts a whole number of 15-minute slots");
        }
        translating(Constraints.TREATMENT_NAME_TAKEN,
                ClinicException.conflict("TREATMENT_EXISTS", "A treatment called '" + t.name + "' already exists"), () -> {
                    treatments.persist(t);
                    treatments.flush();
                });
        return TreatmentView.of(t);
    }

    @Transactional
    public List<TreatmentView> treatments() {
        return treatments.list("order by name, id").stream().map(TreatmentView::of).toList();
    }

    @Transactional
    public PatientView createPatient(PatientRequest request) {
        Patient p = new Patient();
        p.fullName = request.fullName().strip();
        p.email = request.email().strip();
        p.createdAt = clock.instant();
        translating(Constraints.EMAIL_TAKEN, ClinicException.conflict("EMAIL_TAKEN", "A patient with this email already exists"),
                () -> {
                    patients.persist(p);
                    patients.flush();
                });
        return PatientView.of(p);
    }

    @Transactional
    public PatientView patient(long id) {
        return PatientView.of(patients.findByIdOptional(id)
                .orElseThrow(() -> ClinicException.notFound("Patient " + id + " does not exist")));
    }

    @Transactional
    public List<HoursView> workingHours(long practitionerId) {
        requirePractitioner(practitionerId);
        return hours.ofPractitioner(practitionerId).stream().map(HoursView::of).toList();
    }

    /** Replaces the whole week. Two blocks that overlap on the same day are refused by the database. */
    @Transactional
    public List<HoursView> replaceWorkingHours(long practitionerId, List<HoursBlock> blocks) {
        Practitioner practitioner = requirePractitioner(practitionerId);
        for (HoursBlock b : blocks) {
            if (!b.start().isBefore(b.end())) {
                throw ClinicException.unprocessable("INVALID_HOURS", "A block must start before it ends (weekday " + b.weekday() + ")");
            }
        }
        hours.deleteAllOf(practitionerId);
        translating(Constraints.HOURS_OVERLAP,
                ClinicException.unprocessable("OVERLAPPING_HOURS", "Two blocks of the same weekday overlap"), () -> {
                    for (HoursBlock b : blocks) {
                        WorkingHours h = new WorkingHours();
                        h.practitioner = practitioner;
                        h.weekday = (short) b.weekday();
                        h.startTime = b.start();
                        h.endTime = b.end();
                        hours.persist(h);
                    }
                    hours.flush();
                });
        return hours.ofPractitioner(practitionerId).stream().map(HoursView::of).toList();
    }

    private Practitioner requirePractitioner(long id) {
        return practitioners.findByIdOptional(id)
                .orElseThrow(() -> ClinicException.notFound("Practitioner " + id + " does not exist"));
    }

    /**
     * Runs the writes NOW and turns the refusal of one named constraint into a domain error. With {@code IDENTITY} ids
     * the INSERT happens inside {@code persist()}, not at {@code flush()}: the try must cover both.
     */
    private static void translating(String constraint, ClinicException translated, Runnable writes) {
        try {
            writes.run();
        } catch (PersistenceException e) {
            if (Constraints.violated(e).filter(constraint::equals).isPresent()) {
                throw translated;
            }
            throw e;
        }
    }
}
