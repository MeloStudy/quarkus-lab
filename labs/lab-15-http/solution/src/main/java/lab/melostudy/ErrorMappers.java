package lab.melostudy;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.persistence.OptimisticLockException;

public class ErrorMappers {

    /**
     * Two writers read the same version and both tried to write: the database let only one UPDATE through.
     * For the loser that is exactly "the precondition (If-Match) is no longer true": 412.
     */
    @ServerExceptionMapper
    RestResponse<String> staleWrite(OptimisticLockException e) {
        return RestResponse.status(RestResponse.Status.PRECONDITION_FAILED, "the document was changed by someone else");
    }
}
