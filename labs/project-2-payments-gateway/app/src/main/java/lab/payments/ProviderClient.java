package lab.payments;

import java.util.Map;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The card provider. Its URL and timeouts live under {@code quarkus.rest-client.provider.*}. The {@code Idempotency-Key}
 * is what makes it safe to ask twice: the provider answers the same thing and charges once.
 */
@RegisterRestClient(configKey = "provider")
@RegisterProvider(ProviderClientFilter.class)
@Path("/charges")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ProviderClient {

    record ChargeCommand(long amount, String currency, String token) {
    }

    /** {@code status} is {@code captured}, {@code declined} or {@code refunded}. */
    record ChargeAnswer(String id, String status, String reason) {
    }

    @POST
    ChargeAnswer charge(@HeaderParam("Idempotency-Key") String idempotencyKey, ChargeCommand command);

    /** What the provider knows about the charge made with this key; 404 when it never saw it. */
    @GET
    ChargeAnswer lookup(@QueryParam("key") String idempotencyKey);

    @POST
    @Path("/{id}/refund")
    ChargeAnswer refund(@PathParam("id") String chargeId, @HeaderParam("Idempotency-Key") String idempotencyKey);

    /**
     * Sorts the provider's answers by what they mean for us (see {@link ProviderException}). 402 is a normal business
     * answer, not a failure of the provider.
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        int status = response.getStatus();
        if (status == 402) {
            return new ProviderException.Declined(declineReason(response));
        }
        if (status == 404) {
            return new ProviderException.NotFound();
        }
        if (status == 429 || status >= 500) {
            return new ProviderException.Transient("The provider answered HTTP " + status, null);
        }
        if (status >= 400) {
            return new ProviderException.Rejected(status);
        }
        return null;
    }

    private static String declineReason(Response response) {
        try {
            Object reason = response.readEntity(Map.class).get("reason");
            return reason == null ? "declined" : reason.toString();
        } catch (RuntimeException e) {
            return "declined";
        }
    }
}
