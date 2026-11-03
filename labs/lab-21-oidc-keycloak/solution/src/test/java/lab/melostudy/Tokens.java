package lab.melostudy;

import io.quarkus.test.keycloak.client.KeycloakTestClient;

/** Gets real access tokens from the Keycloak that Dev Services started, for the users of {@code lab-realm.json}. */
public final class Tokens {

    private static final KeycloakTestClient KEYCLOAK = new KeycloakTestClient();

    private Tokens() {
    }

    /** A token for the client that this service protects ({@code notes-service}); the password is {@code <user>-pw}. */
    public static String of(String user) {
        return KEYCLOAK.getAccessToken(user, user + "-pw", "notes-service", "notes-secret");
    }

    /** A token for another client of the same realm: a valid token, but not meant for this service. */
    public static String forBilling(String user) {
        return KEYCLOAK.getAccessToken(user, user + "-pw", "billing-service", "billing-secret");
    }
}
