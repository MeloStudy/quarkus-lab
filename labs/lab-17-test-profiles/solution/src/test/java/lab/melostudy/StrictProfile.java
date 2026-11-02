package lab.melostudy;

import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * A test profile is a different APPLICATION: its own configuration and its own start. Every test class annotated with
 * {@code @TestProfile(StrictProfile.class)} shares that one application; any other profile (or none) is another one.
 */
public class StrictProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("app.strict", "true");
    }

    /**
     * Resources declared with an annotation on a test class belong to the tests that carry it. The tests of this
     * profile need the fake rates server too, so the profile attaches it.
     */
    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(FakeRatesResource.class));
    }
}
