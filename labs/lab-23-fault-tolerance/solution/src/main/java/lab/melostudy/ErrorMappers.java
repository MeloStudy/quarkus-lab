package lab.melostudy;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.Response;

/** One place that turns our exceptions into HTTP answers. */
public class ErrorMappers {

    @ServerExceptionMapper
    public Response unknownCity(UnknownCityException e) {
        return error(404, "UNKNOWN_CITY", e.getMessage());
    }

    @ServerExceptionMapper
    public Response unavailable(ShippingUnavailableException e) {
        return error(503, "SHIPPING_UNAVAILABLE", e.getMessage());
    }

    /** Without a fallback, the failure of the carrier reaches the caller as a 503, never as a 500. */
    @ServerExceptionMapper
    public Response carrierFailed(CarrierUnavailableException e) {
        return error(503, "SHIPPING_UNAVAILABLE", e.getMessage());
    }

    /** The fault tolerance timeout fired and nothing else took over. */
    @ServerExceptionMapper
    public Response timedOut(TimeoutException e) {
        return error(504, "UPSTREAM_TIMEOUT", "The carrier took too long");
    }

    @ServerExceptionMapper
    public Response busy(BulkheadException e) {
        return error(503, "BUSY", "Too many concurrent requests");
    }

    private static Response error(int status, String code, String message) {
        return Response.status(status).entity(new ApiError(code, message)).build();
    }
}
