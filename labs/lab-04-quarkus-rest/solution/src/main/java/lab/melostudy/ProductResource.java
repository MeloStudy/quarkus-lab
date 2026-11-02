package lab.melostudy;

import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

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

/** REST API for products, mounted at /products. */
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
            @RestQuery @DefaultValue("0") int page,
            @RestQuery @DefaultValue("10") int size) {
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
    public RestResponse<Product> create(ProductInput input, @Context UriInfo uriInfo) {
        Product created = repository.create(input);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
        return RestResponse.ResponseBuilder.<Product>created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Product update(@RestPath long id, ProductInput input) {
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
