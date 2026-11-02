package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/info")
public class InfoResource {

    // TODO (Tareas 2 y 3): crear un @ConfigMapping (interfaz) con prefijo "app", inyectarlo aquí
    // y devolver un texto con el formato indicado en el README.
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String info() {
        return "TODO";
    }
}
