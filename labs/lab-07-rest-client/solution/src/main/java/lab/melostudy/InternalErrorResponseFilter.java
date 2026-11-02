package lab.melostudy;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

/**
 * Adds the "unexpected failure" response to every operation, so it does not have to be
 * repeated by hand on each endpoint. Registered with {@code mp.openapi.filter}.
 */
public class InternalErrorResponseFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getPaths() == null) {
            return;
        }
        for (PathItem item : openAPI.getPaths().getPathItems().values()) {
            for (Operation operation : item.getOperations().values()) {
                operation.getResponses().addAPIResponse("500", OASFactory.createAPIResponse()
                        .description("Unexpected failure (INTERNAL_ERROR); the errorId is also in the server log")
                        .content(errorContent()));
            }
        }
    }

    private static Content errorContent() {
        MediaType json = OASFactory.createMediaType()
                .schema(OASFactory.createSchema().ref("#/components/schemas/ApiError"));
        return OASFactory.createContent().addMediaType("application/json", json);
    }
}
