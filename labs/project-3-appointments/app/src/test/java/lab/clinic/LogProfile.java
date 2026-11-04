package lab.clinic;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The tests read what the application logs, so they switch on the file handler with JSON output (the console is what
 * production prints; the file holds the same records and can be read back without touching stdout).
 */
public class LogProfile implements QuarkusTestProfile {

    static final String FILE = "target/clinic-test.log";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.log.file.enabled", "true",
                "quarkus.log.file.path", FILE,
                "quarkus.log.file.json.enabled", "true");
    }
}
