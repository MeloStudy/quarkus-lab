package lab.melostudy;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Read-only reports, mounted at /reports. */
@Path("/reports")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reports", description = "Figures computed by the database")
public class ReportResource {

    private final ProductRepository products;

    ReportResource(ProductRepository products) {
        this.products = products;
    }

    @GET
    @Path("/categories")
    @Operation(summary = "Products, stock and average price per category", description = "Sorted by category name.")
    @APIResponse(responseCode = "200", description = "One row per category that has products")
    public List<CategoryStats> categories() {
        return products.categoryStats();
    }
}
