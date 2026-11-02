package lab.melostudy;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    private final ReviewRepository reviews;
    private final ProductMapper productMapper;
    private final ReviewMapper reviewMapper;

    ProductResource(ProductRepository repository, ReviewRepository reviews, ProductMapper productMapper,
            ReviewMapper reviewMapper) {
        this.repository = repository;
        this.reviews = reviews;
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
    }

    @GET
    public RestResponse<List<ProductResponse>> list(
            @RestQuery String category,
            @RestQuery String label,
            @RestQuery @Size(max = 100) String text,
            @RestQuery @DecimalMin("0") BigDecimal minPrice,
            @RestQuery @DecimalMin("0") BigDecimal maxPrice,
            @RestQuery @DefaultValue("0") @Min(0) int page,
            @RestQuery @DefaultValue("10") @Min(1) @Max(100) int size) {
        ProductRepository.ProductPage result = repository.page(category, label, text, minPrice, maxPrice, page, size);
        return RestResponse.ResponseBuilder.ok(productMapper.toResponses(result.items()))
                .header("X-Total-Count", result.total())
                .build();
    }

    @GET
    @Path("/{id}")
    public ProductResponse get(@RestPath long id) {
        return productMapper.toResponse(repository.findOne(id).orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/{id}/price-history")
    public List<PriceChangeResponse> priceHistory(@RestPath long id) {
        repository.findOne(id).orElseThrow(NotFoundException::new);
        return productMapper.toPriceChangeResponses(PriceChange.historyOf(id));
    }

    @PUT
    @Path("/{id}/labels")
    public ProductResponse setLabels(@RestPath long id,
            @NotNull @Size(max = 10) List<@NotBlank @Pattern(regexp = "[a-z0-9-]{1,30}") String> names) {
        return productMapper.toResponse(repository.setLabels(id, names).orElseThrow(NotFoundException::new));
    }

    @GET
    @Path("/{id}/reviews")
    public List<ReviewResponse> reviews(@RestPath long id) {
        repository.findOne(id).orElseThrow(NotFoundException::new);
        return reviewMapper.toResponses(reviews.of(id));
    }

    @GET
    @Path("/{id}/review-summary")
    public ReviewSummary reviewSummary(@RestPath long id) {
        repository.findOne(id).orElseThrow(NotFoundException::new);
        return reviews.summaryOf(id);
    }

    @POST
    @Path("/{id}/reviews")
    public RestResponse<ReviewResponse> addReview(@RestPath long id, @Valid @NotNull ReviewInput input) {
        Review created = reviews.add(id, input).orElseThrow(NotFoundException::new);
        return RestResponse.ResponseBuilder.<ReviewResponse>created(URI.create("/products/" + id + "/reviews"))
                .entity(reviewMapper.toResponse(created)).build();
    }

    @POST
    public RestResponse<ProductResponse> create(@Valid @NotNull ProductInput input, @Context UriInfo uriInfo) {
        Product created = repository.create(input);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id)).build();
        return RestResponse.ResponseBuilder.<ProductResponse>created(location).entity(productMapper.toResponse(created)).build();
    }

    @PUT
    @Path("/{id}")
    public ProductResponse update(@RestPath long id, @Valid @NotNull ProductInput input) {
        return productMapper.toResponse(repository.update(id, input).orElseThrow(NotFoundException::new));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@RestPath long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException();
        }
    }
}
