package lab.melostudy;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

/**
 * Document-wide information that does not belong to any single endpoint: tag descriptions and
 * reusable error responses (referenced from the resource with {@code @APIResponse(ref = "...")}).
 * <p>{@code @OpenAPIDefinition} is only honored on a JAX-RS {@code Application} subclass.
 * The API title, version and description are declared here too; they can be overridden per
 * environment with {@code quarkus.smallrye-openapi.info-*} properties.
 */
@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(title = "Product Catalog API", version = "1.0.0",
                description = "Create, read, update and delete the products of the catalog.",
                contact = @Contact(name = "Catalog team", email = "catalog@lab.melostudy")),
        tags = @Tag(name = "Products", description = "Create, read, update and delete catalog products"),
        components = @Components(responses = {
                @APIResponse(name = "ValidationError", responseCode = "400",
                        description = "The request is not valid (VALIDATION_ERROR or MALFORMED_REQUEST)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))),
                @APIResponse(name = "NotFound", responseCode = "404",
                        description = "The product does not exist (NOT_FOUND)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))),
                @APIResponse(name = "Conflict", responseCode = "409",
                        description = "A product with that name already exists (CONFLICT), or there is not enough stock (OUT_OF_STOCK)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))),
                @APIResponse(name = "PaymentRefused", responseCode = "402",
                        description = "The payment was refused and the order was not placed (PAYMENT_REFUSED)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))),
                @APIResponse(name = "UpstreamError", responseCode = "502",
                        description = "A dependency failed or is unreachable (UPSTREAM_ERROR)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))),
                @APIResponse(name = "UpstreamTimeout", responseCode = "504",
                        description = "A dependency did not answer in time (UPSTREAM_TIMEOUT)",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class)))
        }))
public class ApiDocumentation extends Application {
}
