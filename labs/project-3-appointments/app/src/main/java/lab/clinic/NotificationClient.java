package lab.clinic;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** The notification provider, as a typed REST client (lab 7). URL and timeouts: {@code quarkus.rest-client.notifications.*}. */
@RegisterRestClient(configKey = "notifications")
@Path("/messages")
@ClientHeaderParam(name = "Authorization", value = "Bearer ${notifications.api-key}")
public interface NotificationClient {

    record Message(String to, String text) {
    }

    /** The same {@code Idempotency-Key} is the same message: a provider that answers late or not at all can be asked again. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response send(@HeaderParam("Idempotency-Key") String idempotencyKey, Message message);
}
