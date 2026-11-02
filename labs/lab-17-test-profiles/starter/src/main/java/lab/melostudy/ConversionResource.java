package lab.melostudy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code GET /convert?amount=100&from=USD&to=EUR}: rate from the remote service, fee from the database, result stored.
 * In strict mode ({@code app.strict=true}) a pair without a fee entry is refused with 422 BEFORE the remote service
 * is asked; otherwise it is converted with no fee.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ConversionResource {

    private static final Pattern CODE = Pattern.compile("[A-Z]{3}");

    public record Quote(String from, String to, BigDecimal amount, BigDecimal rate, BigDecimal feePercent, BigDecimal result) {
    }

    public record HistoryEntry(String from, String to, BigDecimal amount, BigDecimal result) {
    }

    private final RatesClient rates;
    private final FeeRepository fees;
    private final boolean strict;

    ConversionResource(RatesClient rates, FeeRepository fees, @ConfigProperty(name = "app.strict") boolean strict) {
        this.rates = rates;
        this.fees = fees;
        this.strict = strict;
    }

    @GET
    @Path("/convert")
    @Transactional
    public Quote convert(@RestQuery BigDecimal amount, @RestQuery String from, @RestQuery String to) {
        if (amount == null || amount.signum() <= 0 || from == null || to == null
                || !CODE.matcher(from).matches() || !CODE.matcher(to).matches()) {
            throw new BadRequestException("amount > 0 and three-letter codes are required");
        }
        Optional<BigDecimal> fee = fees.percent(from, to);
        if (strict && fee.isEmpty()) {
            throw new UnsupportedPairException(from, to);
        }
        BigDecimal rate = rates.rate(from, to).orElseThrow(NotFoundException::new);
        BigDecimal feePercent = fee.orElse(BigDecimal.ZERO);
        BigDecimal result = amount.multiply(rate)
                .multiply(BigDecimal.valueOf(100).subtract(feePercent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);

        Conversion conversion = new Conversion();
        conversion.fromCode = from;
        conversion.toCode = to;
        conversion.amount = amount;
        conversion.result = result;
        conversion.createdAt = Instant.now();
        conversion.persist();
        return new Quote(from, to, amount, rate, feePercent, result);
    }

    @GET
    @Path("/history")
    public List<HistoryEntry> history() {
        return Conversion.<Conversion>listAll(Sort.by("id")).stream()
                .map(c -> new HistoryEntry(c.fromCode, c.toCode, c.amount, c.result)).toList();
    }

    @ServerExceptionMapper
    RestResponse<String> unavailable(RatesUnavailableException e) {
        return RestResponse.status(RestResponse.Status.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ServerExceptionMapper
    RestResponse<String> unsupported(UnsupportedPairException e) {
        return RestResponse.status(422, e.getMessage());
    }

    public static class UnsupportedPairException extends RuntimeException {
        public UnsupportedPairException(String from, String to) {
            super("no fee is defined for " + from + " -> " + to);
        }
    }
}
