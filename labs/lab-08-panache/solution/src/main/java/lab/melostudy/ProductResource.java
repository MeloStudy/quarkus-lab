package lab.melostudy;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Tag(name = "Products", description = "Create, read, update and delete catalog products")
public class ProductResource {

    private final ProductRepository repository;
    private final PricingService pricing;

    ProductResource(ProductRepository repository, PricingService pricing) {
        this.repository = repository;
        this.pricing = pricing;
    }

    @GET
    @Operation(summary = "List products",
            description = "Returns one page of products, optionally filtered by category. Sorted by id.")
    @APIResponse(responseCode = "200", description = "A page of products (possibly empty)",
            headers = @Header(name = "X-Total-Count", description = "Total number of matching products, before paging",
                    schema = @Schema(type = SchemaType.INTEGER)))
    @APIResponse(responseCode = "400", ref = "ValidationError")
    public RestResponse<List<Product>> list(
            @Parameter(description = "Only products of this category (case-insensitive)") @RestQuery String category,
            @Parameter(description = "Zero-based page number") @RestQuery @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size (1 to 100)") @RestQuery @DefaultValue("10") @Min(1) @Max(100) int size) {
        ProductRepository.ProductPage result = repository.page(category, page, size);
        return RestResponse.ResponseBuilder.ok(result.items())
                .header("X-Total-Count", result.total())
                .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a product by id")
    @APIResponse(responseCode = "200", description = "The product")
    @APIResponse(responseCode = "404", ref = "NotFound")
    public Product get(@RestPath long id) {
        return repository.findOne(id).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id}/price")
    @Operation(summary = "Get a product price in another currency",
            description = "Converts the USD price with a rate fetched from the remote exchange-rate service.")
    @APIResponse(responseCode = "200", description = "The converted price")
    @APIResponse(responseCode = "400", ref = "ValidationError")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "502", ref = "UpstreamError")
    @APIResponse(responseCode = "504", ref = "UpstreamTimeout")
    public PriceQuote price(@RestPath long id,
            @Parameter(description = "Target currency, three uppercase letters (ISO 4217)", example = "EUR")
            @RestQuery @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {
        return pricing.quote(id, currency).orElseThrow(NotFoundException::new);
    }

    @GET
    @Path("/{id}/price-history")
    @Operation(summary = "List the price changes of a product", description = "Newest first.")
    @APIResponse(responseCode = "200", description = "The recorded price changes (possibly empty)")
    @APIResponse(responseCode = "404", ref = "NotFound")
    public List<PriceChange> priceHistory(@RestPath long id) {
        repository.findOne(id).orElseThrow(NotFoundException::new);
        return PriceChange.historyOf(id);
    }

    @POST
    @Operation(summary = "Create a product", description = "Product names are unique (case-insensitive).")
    @APIResponse(responseCode = "201", description = "Created; the Location header points at the new product",
            headers = @Header(name = "Location", description = "URL of the created product",
                    schema = @Schema(type = SchemaType.STRING)),
            content = @Content(schema = @Schema(implementation = Product.class)))
    @APIResponse(responseCode = "400", ref = "ValidationError")
    @APIResponse(responseCode = "409", ref = "Conflict")
    public RestResponse<Product> create(@Valid @NotNull ProductInput input, @Context UriInfo uriInfo) {
        Product created = repository.create(input);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build();
        return RestResponse.ResponseBuilder.<Product>created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Replace a product", description = "Replaces every field of the product with the given values.")
    @APIResponse(responseCode = "200", description = "The updated product")
    @APIResponse(responseCode = "400", ref = "ValidationError")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "409", ref = "Conflict")
    public Product update(@RestPath long id, @Valid @NotNull ProductInput input) {
        return repository.update(id, input).orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a product")
    @APIResponse(responseCode = "204", description = "Deleted")
    @APIResponse(responseCode = "404", ref = "NotFound")
    public void delete(@RestPath long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException();
        }
    }
}
