package lab.clinic;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import lab.clinic.Requests.PractitionerRequest;
import lab.clinic.Requests.WorkingHoursRequest;
import lab.clinic.Responses.AvailabilityView;
import lab.clinic.Responses.HoursView;
import lab.clinic.Responses.PractitionerView;

@Path("/api/practitioners")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PractitionerResource {

    private final CatalogService catalog;
    private final AvailabilityService availability;

    PractitionerResource(CatalogService catalog, AvailabilityService availability) {
        this.catalog = catalog;
        this.availability = availability;
    }

    @POST
    public Response create(@Valid @NotNull PractitionerRequest request) {
        PractitionerView created = catalog.createPractitioner(request);
        return Response.created(URI.create("/api/practitioners/" + created.id())).entity(created).build();
    }

    @GET
    public List<PractitionerView> list() {
        return catalog.practitioners();
    }

    @GET
    @Path("/{id}")
    public PractitionerView get(@RestPath long id) {
        return catalog.practitioner(id);
    }

    @GET
    @Path("/{id}/working-hours")
    public List<HoursView> hours(@RestPath long id) {
        return catalog.workingHours(id);
    }

    @PUT
    @Path("/{id}/working-hours")
    public List<HoursView> replaceHours(@RestPath long id, @Valid @NotNull WorkingHoursRequest request) {
        return catalog.replaceWorkingHours(id, request.blocks());
    }

    /**
     * The date and the treatment id arrive as validated TEXT (the 404-on-conversion trap of lab 5/P1): a mistake of the
     * client is a 400 that names the parameter.
     */
    @GET
    @Path("/{id}/availability")
    public AvailabilityView availability(@RestPath long id,
            @RestQuery @NotNull @Pattern(regexp = "[0-9]{4}-[0-9]{2}-[0-9]{2}") String date,
            @RestQuery @NotNull @Pattern(regexp = "[0-9]{1,18}") String treatmentId) {
        return availability.availability(id, date, Long.parseLong(treatmentId));
    }
}
