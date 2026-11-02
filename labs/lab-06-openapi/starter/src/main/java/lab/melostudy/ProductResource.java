package lab.melostudy;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST API for products, mounted at /products.
 *
 * TODO (Task 1): add the smallrye-openapi extension (nothing else to write for that task).
 * TODO (Task 2): document the API as a whole (title, version, description) and group this
 *                resource under a documented "Products" tag.
 * TODO (Task 3): give every operation a real summary and describe the query parameters.
 * TODO (Task 4): make the responses tell the truth: create answers 201 + Location; list sends
 *                X-Total-Count; document 400/404/409 with the ApiError schema.
 * TODO (Task 5): describe and give examples for the fields of Product, ProductInput and ApiError.
 * TODO (Task 6): document the 500 response of every operation once, with a filter.
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private final ProductRepository repository;

    ProductResource(ProductRepository repository) {
        this.repository = repository;
    }

    @GET
    public RestResponse<List<Product>> list(@RestQuery String category,
            @RestQuery @DefaultValue("0") @Min(0) int page,
            @RestQuery @DefaultValue("10") @Min(1) @Max(100) int size) {
        List<Product> matching = repository.findAll(category);
        List<Product> pageItems = matching.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();
        return RestResponse.ResponseBuilder.ok(pageItems)
                .header("X-Total-Count", matching.size())
                .build();
    }

    @GET
    @Path("/{id}")
    public Product get(@RestPath long id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    @POST
    public RestResponse<Product> create(@Valid @NotNull ProductInput input, @Context UriInfo uriInfo) {
        Product created = repository.create(input);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
        return RestResponse.ResponseBuilder.<Product>created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Product update(@RestPath long id, @Valid @NotNull ProductInput input) {
        return repository.update(id, input).orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@RestPath long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException();
        }
    }
}
