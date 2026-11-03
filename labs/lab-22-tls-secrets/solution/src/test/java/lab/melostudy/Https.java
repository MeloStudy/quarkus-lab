package lab.melostudy;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.specification.RequestSpecification;

/** Helpers to call the application over HTTPS, trusting the certificate that {@link LabCertificates} generated. */
@QuarkusTestResource(LabCertificates.class)
public final class Https {

    public static final int HTTPS_PORT = 8444;
    public static final int HTTP_PORT = 8081;

    private Https() {
    }

    public static RequestSpecification request() {
        return given()
                .baseUri("https://localhost")
                .port(HTTPS_PORT)
                .trustStore(LabCertificates.DIRECTORY.resolve("lab-truststore.p12").toFile(), LabCertificates.PASSWORD);
    }
}
