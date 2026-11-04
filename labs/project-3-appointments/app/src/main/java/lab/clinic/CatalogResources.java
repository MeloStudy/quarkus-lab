package lab.clinic;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;

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
        public Response create(@Valid @NotNull TreatmentRequest request) {
            TreatmentView created = catalog.createTreatment(request);
            return Response.created(URI.create("/api/treatments/" + created.id())).entity(created).build();
        }

        @GET
        public List<TreatmentView> list() {
            return catalog.treatments();
        }
    }

    @Path("/api/patients")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class PatientResource {

        private final CatalogService catalog;

        PatientResource(CatalogService catalog) {
            this.catalog = catalog;
        }

        @POST
        public Response create(@Valid @NotNull PatientRequest request) {
            PatientView created = catalog.createPatient(request);
            return Response.created(URI.create("/api/patients/" + created.id())).entity(created).build();
        }

        @GET
        @Path("/{id}")
        public PatientView get(@RestPath long id) {
            return catalog.patient(id);
        }
    }
}
