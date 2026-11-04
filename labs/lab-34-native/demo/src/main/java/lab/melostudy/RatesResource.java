package lab.melostudy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Serves a file from the classpath: a very common way to ship reference data. */
@Path("/rates")
public class RatesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String rates() throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("rates.json")) {
            if (in == null) {
                throw new NotFoundException("rates.json is not in the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
