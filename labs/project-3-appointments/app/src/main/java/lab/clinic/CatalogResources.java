package lab.clinic;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lab.clinic.Requests.PatientRequest;
import lab.clinic.Requests.TreatmentRequest;
import lab.clinic.Responses.PatientView;
import lab.clinic.Responses.TreatmentView;

/** The two small reference resources: what is done here, and who comes. */
final class CatalogResources {

    private CatalogResources() {
    }

    @Path("/api/treatments")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class TreatmentResource {

        private final CatalogService catalog;

        TreatmentResource(CatalogService catalog) {
            this.catalog = catalog;
        }

        @POST
        @RolesAllowed(Caller.RECEPTION)
        public Response create(@Valid @NotNull TreatmentRequest request) {
            TreatmentView created = catalog.createTreatment(request);
            return Response.created(URI.create("/api/treatments/" + created.id())).entity(created).build();
        }

        @GET
        @RolesAllowed({ Caller.RECEPTION, Caller.PRACTITIONER, Caller.PATIENT })
        public List<TreatmentView> list() {
            return catalog.treatments();
        }
    }

    @Path("/api/patients")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class PatientResource {

        private final CatalogService catalog;
        private final Caller caller;

        PatientResource(CatalogService catalog, Caller caller) {
            this.catalog = catalog;
            this.caller = caller;
        }

        @POST
        @RolesAllowed(Caller.RECEPTION)
        public Response create(@Valid @NotNull PatientRequest request) {
            PatientView created = catalog.createPatient(request);
            return Response.created(URI.create("/api/patients/" + created.id())).entity(created).build();
        }

        /** Reception reads any patient; a patient reads only themselves, and anybody else is a 404. */
        @GET
        @Path("/{id}")
        @RolesAllowed({ Caller.RECEPTION, Caller.PATIENT })
        public PatientView get(@RestPath long id) {
            if (!caller.isReception() && !caller.patientId().filter(own -> own == id).isPresent()) {
                throw ClinicException.notFound("Patient " + id + " does not exist");
            }
            return catalog.patient(id);
        }
    }
}
