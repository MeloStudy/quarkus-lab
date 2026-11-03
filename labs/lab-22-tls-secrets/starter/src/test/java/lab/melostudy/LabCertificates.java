package lab.melostudy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

/**
 * Creates a throw-away self-signed certificate for {@code localhost} before Quarkus starts:
 * {@code target/certs/lab-keystore.p12} (the server's) and {@code target/certs/lab-truststore.p12} (what a client
 * must trust). Nothing is committed: the files are generated at every build and die with {@code mvn clean}.
 */
public class LabCertificates implements QuarkusTestResourceLifecycleManager {

    public static final Path DIRECTORY = Path.of("target", "certs");
    public static final String PASSWORD = "lab-pw";

    @Override
    public Map<String, String> start() {
        try {
            Files.createDirectories(DIRECTORY);
            new CertificateGenerator(DIRECTORY, true).generate(new CertificateRequest()
                    .withName("lab")
                    .withPassword(PASSWORD)
                    .withFormat(Format.PKCS12)
                    .withCN("localhost")
                    .withSubjectAlternativeName("DNS:localhost")
                    .withDuration(Duration.ofDays(2)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate the test certificate", e);
        }
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
