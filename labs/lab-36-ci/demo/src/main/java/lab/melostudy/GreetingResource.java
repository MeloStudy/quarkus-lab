package lab.melostudy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @Inject
    Greeter greeter;

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String greet(@PathParam("name") String name) {
        return greeter.greet(name);
    }
}
