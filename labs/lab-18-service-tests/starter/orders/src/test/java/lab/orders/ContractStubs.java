package lab.orders;

import com.github.tomakehurst.wiremock.WireMockServer;

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

    /**
     * TODO task 2: register in WireMock a stub that answers exactly what the contract says: the same method and path,
     * the same request body when the interaction has one (compare it as JSON, not as text), and the response with its
     * status, a JSON content type and its body.
     */
    public static void stub(WireMockServer wireMock, Contract.Interaction interaction) {
        throw new UnsupportedOperationException("TODO task 2");
    }
}
