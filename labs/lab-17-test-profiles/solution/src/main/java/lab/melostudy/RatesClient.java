package lab.melostudy;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/** Asks a remote service for an exchange rate: {@code GET <rates.url>/rates?from=USD&to=EUR} answers the rate as plain text. */
@ApplicationScoped
public class RatesClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    RatesClient(@ConfigProperty(name = "rates.url") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Empty when the remote service does not know the pair; fails with {@link RatesUnavailableException} when it cannot answer. */
    public Optional<BigDecimal> rate(String from, String to) {
        URI uri = URI.create(baseUrl + "/rates?from=" + enc(from) + "&to=" + enc(to));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                throw new RatesUnavailableException("rates service answered " + response.statusCode());
            }
            return Optional.of(new BigDecimal(response.body().trim()));
        } catch (IOException | NumberFormatException e) {
            throw new RatesUnavailableException("rates service not reachable: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RatesUnavailableException("interrupted");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
