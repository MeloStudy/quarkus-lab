package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Loads a plugin by its name: nothing in the code mentions the plugin classes. */
@Path("/plugins")
public class PluginResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String greet(@PathParam("name") String name) throws Exception {
        try {
            Plugin plugin = (Plugin) Class.forName("lab.melostudy.plugins." + name + "Plugin").getDeclaredConstructor().newInstance();
            return plugin.greet();
        } catch (ClassNotFoundException e) {
            throw new NotFoundException("No plugin called " + name);
        }
    }
}
