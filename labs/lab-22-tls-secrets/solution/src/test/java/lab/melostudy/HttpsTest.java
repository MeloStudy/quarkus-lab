package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/** Task 1: the service speaks TLS 1.3 with the certificate of its keystore and sends plain HTTP clients to HTTPS. */
@QuarkusTest
class HttpsTest {

    @Test
    void answersOverHttps() {
        Https.request().get("/api/hello").then().statusCode(200).body("message", equalTo("hello"));
    }

    @Test
    void plainHttpIsRedirectedToHttps() {
        Https.request().get("/api/hello").then().statusCode(200);
        given().redirects().follow(false).get("http://localhost:" + Https.HTTP_PORT + "/api/hello")
                .then()
                .statusCode(org.hamcrest.Matchers.anyOf(equalTo(301), equalTo(302), equalTo(307), equalTo(308)))
                .header("Location", startsWith("https://localhost:" + Https.HTTPS_PORT + "/api/hello"));
    }

    @Test
    void aClientWithoutTheCertificateInItsTruststoreIsRefused() {
        Https.request().get("/api/hello").then().statusCode(200);
        assertThrows(SSLException.class, () -> given()
                .baseUri("https://localhost").port(Https.HTTPS_PORT)
                .get("/api/hello"));
    }

    @Test
    void negotiatesTls13() throws Exception {
        try (SSLSocket socket = socketWith("TLSv1.3")) {
            socket.startHandshake();
            assertEquals("TLSv1.3", socket.getSession().getProtocol());
        }
    }

    @Test
    void refusesTls12() throws Exception {
        try (SSLSocket socket = socketWith("TLSv1.3")) {
            socket.startHandshake();
        }
        try (SSLSocket socket = socketWith("TLSv1.2")) {
            assertThrows(SSLException.class, socket::startHandshake);
        }
    }

    private static SSLSocket socketWith(String protocol) throws Exception {
        KeyStore trust = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(LabCertificates.DIRECTORY.resolve("lab-truststore.p12"))) {
            trust.load(in, LabCertificates.PASSWORD.toCharArray());
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, factory.getTrustManagers(), null);
        SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket("localhost", Https.HTTPS_PORT);
        socket.setEnabledProtocols(new String[] { protocol });
        return socket;
    }
}
