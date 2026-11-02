package lab.melostudy;

import java.math.BigDecimal;
import java.net.URI;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/rewards")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RewardsResource {

    public record OrderInput(String customerId, BigDecimal amount) {
    }

    public record Balance(String customerId, int points) {
    }

    private final RewardsService service;
    private final AccountRepository accounts;

    RewardsResource(RewardsService service, AccountRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @POST
    @Path("/orders")
    public RestResponse<RewardsService.Award> award(OrderInput input) {
        RewardsService.Award award = service.award(input == null ? null : input.customerId(), input == null ? null : input.amount());
        return RestResponse.ResponseBuilder.<RewardsService.Award>created(URI.create("/rewards/" + award.customerId()))
                .entity(award).build();
    }

    @GET
    @Path("/{customerId}")
    public Balance balance(@RestPath String customerId) {
        return accounts.findByCustomer(customerId).map(a -> new Balance(a.customerId, a.points))
                .orElseThrow(NotFoundException::new);
    }

    @ServerExceptionMapper
    RestResponse<String> suspicious(SuspiciousOrderException e) {
        return RestResponse.status(RestResponse.Status.CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> fraudDown(FraudServiceUnavailableException e) {
        return RestResponse.status(RestResponse.Status.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> invalid(IllegalArgumentException e) {
        return RestResponse.status(RestResponse.Status.BAD_REQUEST, e.getMessage());
    }
}
