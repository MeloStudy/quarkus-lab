package lab.payments;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The HTTP layer: who is asking (the token), what they may do (the roles), and how an outcome is told in HTTP. The
 * rules are in {@link PaymentService}.
 */
@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Inject
    PaymentService service;

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity identity;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("customer")
    public Response create(@HeaderParam("Idempotency-Key") String idempotencyKey, @Valid PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentException(400, "IDEMPOTENCY_KEY_REQUIRED",
                    "Send the header Idempotency-Key: a value you choose and repeat when you retry the same payment");
        }
        if (!SAFE_KEY.matcher(idempotencyKey).matches()) {
            throw new PaymentException(400, "INVALID_IDEMPOTENCY_KEY",
                    "The Idempotency-Key must be 1 to 64 letters, digits, dots, underscores or hyphens");
        }
        PaymentService.Outcome outcome = service.create(jwt.getSubject(), idempotencyKey, request);
        return tell(outcome);
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ "customer", "finance" })
    public PaymentResponse get(@PathParam("id") String id) {
        return PaymentResponse.from(service.get(id, jwt.getSubject(), isFinance()));
    }

    @GET
    @RolesAllowed({ "customer", "finance" })
    public List<PaymentResponse> list(
            @RestQuery @jakarta.validation.constraints.Pattern(regexp = "PENDING|CAPTURED|DECLINED|UNKNOWN|FAILED|REFUNDED",
                    flags = Flag.CASE_INSENSITIVE) String status) {
        PaymentStatus wanted = status == null ? null : PaymentStatus.valueOf(status.toUpperCase());
        return service.list(jwt.getSubject(), isFinance(), wanted).stream().map(PaymentResponse::from).toList();
    }

    @POST
    @Path("{id}/reconcile")
    @RolesAllowed("finance")
    public PaymentResponse reconcile(@PathParam("id") String id) {
        return PaymentResponse.from(service.reconcile(id));
    }

    @POST
    @Path("{id}/refund")
    @RolesAllowed("finance")
    public PaymentResponse refund(@PathParam("id") String id) {
        return PaymentResponse.from(service.refund(id));
    }

    private boolean isFinance() {
        return identity.hasRole("finance");
    }

    /** How each outcome of a payment request is told. A repeated request gets the same words and a marker header. */
    private static Response tell(PaymentService.Outcome outcome) {
        Payment p = outcome.payment();
        Response.ResponseBuilder builder = switch (p.status()) {
            case CAPTURED, REFUNDED -> Response.status(201).entity(PaymentResponse.from(p)).location(URI.create("/api/payments/" + p.id()));
            case UNKNOWN -> Response.status(202).entity(PaymentResponse.from(p)).location(URI.create("/api/payments/" + p.id()));
            case DECLINED -> Response.status(402).entity(ApiError.forPayment("CARD_DECLINED",
                    "The card was declined: " + p.declineReason(), p.id()));
            case FAILED -> Response.status(502).entity(ApiError.forPayment("PROVIDER_REJECTED",
                    "The provider could not process the payment; nobody was charged", p.id()));
            case PENDING -> throw new IllegalStateException("A pending payment is never told");
        };
        if (outcome.replayed()) {
            builder.header("Idempotent-Replayed", "true");
        }
        return builder.build();
    }
}
