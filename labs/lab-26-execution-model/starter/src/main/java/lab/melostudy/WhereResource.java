package lab.melostudy;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Task 1: five endpoints that answer where they ran. What each one needs is in {@code ThreadKindTest}. */
@Path("/where")
@Produces(MediaType.APPLICATION_JSON)
public class WhereResource {

    @Inject
    LegacyCatalog catalog;

    /** Nothing special: the framework decides from the return type. */
    @GET
    @Path("plain")
    public Where plain() {
        return Where.here();
    }

    /** Nothing special either. */
    @GET
    @Path("uni")
    public Uni<Where> uni() {
        return Uni.createFrom().item(Where::here);
    }

    /** TODO Task 1. Returns a {@code Uni}, but the work inside is a blocking driver call. */
    @GET
    @Path("legacy")
    public Uni<Where> legacy() {
        return Uni.createFrom().item(() -> {
            catalog.lookup("A-1");
            return Where.here();
        });
    }

    /** TODO Task 1. Returns a plain object, but the work is a few arithmetic operations: no waiting at all. */
    @GET
    @Path("cheap")
    public Where cheap() {
        return Where.here();
    }

    /** TODO Task 1. Blocking by nature, and many of these may wait at once. */
    @GET
    @Path("virtual")
    public Where virtual() {
        catalog.lookup("A-1");
        return Where.here();
    }
}
