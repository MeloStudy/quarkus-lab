package lab.melostudy;

import org.jboss.resteasy.reactive.RestPath;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

/** Documents, API version 2 (read only here). Same data, new representation; version 1 keeps working. */
@Path("/api/v2/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentV2Resource {

    @GET
    @Path("/{id}")
    public Response get(@RestPath long id, @Context Request request) {
        Document document = Document.findById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        Response.ResponseBuilder notModified = request.evaluatePreconditions(DocumentResource.tagOf(document));
        if (notModified != null) {
            return notModified.build();
        }
        return Response.ok(DocumentV2.of(document)).tag(DocumentResource.tagOf(document)).build();
    }
}
