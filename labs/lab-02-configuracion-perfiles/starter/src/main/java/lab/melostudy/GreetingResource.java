package lab.melostudy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    // TODO (Tarea 1): leer la configuración con @ConfigProperty en lugar de usar valores fijos
    //   greeting.message      -> obligatorio (ya está en application.properties)
    //   greeting.name         -> opcional (Optional<String>); si falta se usa "mundo"
    //   greeting.punctuation  -> valor por defecto "!"
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return "Hola, mundo!";
    }
}
