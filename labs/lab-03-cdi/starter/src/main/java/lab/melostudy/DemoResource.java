package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/demo")
public class DemoResource {

    private final RequestContextInfo requestInfo;

    DemoResource(RequestContextInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    @GET
    @Path("/request-id")
    @Produces(MediaType.TEXT_PLAIN)
    public String requestId() {
        return requestInfo.id();
    }
}
