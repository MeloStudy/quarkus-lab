package lab.melostudy;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Read-only reports, mounted at /reports. */
@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
public class ReportResource {

    private final ProductRepository products;

    ReportResource(ProductRepository products) {
        this.products = products;
    }

    @GET
    @Path("/categories")
    public List<CategoryStats> categories() {
        return products.categoryStats();
    }
}
