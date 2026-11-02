package lab.melostudy;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * A test profile is a different APPLICATION: its own configuration and its own start. Every test class annotated with
 * {@code @TestProfile(StrictProfile.class)} shares that one application.
 * TODO task 3: this profile must (a) turn strict mode on and (b) bring the fake rates server, because the tests of
 * this profile do not declare it themselves.
 */
public class StrictProfile implements QuarkusTestProfile {
}
