package lab.melostudy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/info")
public class InfoResource {

    @Inject
    AppConfig config;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String info() {
        return "%s v%s | tags=%s | contact=%s | url=%s".formatted(
            config.name(),
            config.version(),
            String.join(",", config.tags()),
            config.contact().email(),
            config.baseUrl());
    }
}
