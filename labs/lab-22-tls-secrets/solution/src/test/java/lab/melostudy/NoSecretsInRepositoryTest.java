package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Task 3: a poor man's secret scanner for what ends up inside the jar. In {@code src/main/resources}, a property
 * whose name looks like a secret may only have a value if it belongs to the {@code %dev} or {@code %test} profile
 * (a throw-away value) or is a placeholder such as {@code ${SOME_VARIABLE}}. Real projects add a scanner such as
 * gitleaks to the pipeline; this test is the part that knows your property names.
 */
class NoSecretsInRepositoryTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Pattern SECRET_NAME = Pattern.compile("(?i).*(password|passwd|secret|api[-_.]?key|token|credential|private[-_.]?key).*");

    @Test
    void scannerFindsTheFilesItMustCheck() throws IOException {
        assertTrue(Files.exists(RESOURCES.resolve("application.properties")), "the scanner looks in the wrong folder");
    }

    @Test
    void noSecretHasAValueOutsideTheThrowAwayProfiles() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".properties")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.strip();
                    int equals = trimmed.indexOf('=');
                    if (trimmed.startsWith("#") || equals < 0) {
                        continue;
                    }
                    String name = trimmed.substring(0, equals).strip();
                    String value = trimmed.substring(equals + 1).strip();
                    boolean throwAwayProfile = name.startsWith("%dev.") || name.startsWith("%test.");
                    boolean placeholder = value.startsWith("${") && value.endsWith("}");
                    if (SECRET_NAME.matcher(name).matches() && !value.isEmpty() && !throwAwayProfile && !placeholder) {
                        offenders.add(file.getFileName() + ": " + name);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "secrets with a real value in the repository: " + offenders);
    }

    @Test
    void theNamePatternRecognizesTheUsualSuspects() {
        for (String name : List.of("app.api-key", "quarkus.tls.key-store.p12.password", "my.client.secret", "x.access-token")) {
            assertTrue(SECRET_NAME.matcher(name).matches(), name);
        }
        assertFalse(SECRET_NAME.matcher("app.name").matches());
    }
}
