package lab.melostudy;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Tells clients of version 1 that it is going away: {@code Deprecation: true} now and a {@code Sunset} date
 * (the day it will stop working). A response filter keeps this out of the resources.
 */
@Provider
public class DeprecationFilter implements ContainerResponseFilter {

    private final String sunset;

    DeprecationFilter(@ConfigProperty(name = "api.v1.sunset") String sunset) {
        this.sunset = sunset;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (request.getUriInfo().getPath().startsWith("/api/v1/")) {
            response.getHeaders().putSingle("Deprecation", "true");
            response.getHeaders().putSingle("Sunset", sunset);
        }
    }
}
