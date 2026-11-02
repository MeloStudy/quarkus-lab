package lab.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;

/**
 * Turns one interaction of the contract into a WireMock stub. The consumer's tests then run against EXACTLY what the
 * provider promised, and when the contract changes, these stubs change with it.
 */
public final class ContractStubs {

    private ContractStubs() {
    }

    public static void stub(WireMockServer wireMock, String interactionName) {
        stub(wireMock, Contract.load().stream().filter(i -> i.name().equals(interactionName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no interaction called '" + interactionName + "'")));
    }

    public static void stub(WireMockServer wireMock, Contract.Interaction interaction) {
        MappingBuilder mapping = request(interaction.request().method(), urlEqualTo(interaction.request().path()));
        if (interaction.request().body() != null) {
            mapping = mapping.withRequestBody(equalToJson(interaction.request().body().toString()));
        }
        wireMock.stubFor(mapping.willReturn(aResponse()
                .withStatus(interaction.response().status())
                .withHeader("Content-Type", "application/json")
                .withBody(interaction.response().body().toString())));
    }
}
