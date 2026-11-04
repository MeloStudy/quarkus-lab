package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** A plain unit test: no Quarkus, so it runs in milliseconds. */
class GreeterTest {

    private Greeter greeterWith(String prefix) {
        Greeter greeter = new Greeter();
        greeter.prefix = prefix;
        return greeter;
    }

    @Test
    void greetsByName() {
        assertEquals("Hi, Ana!", greeterWith("Hi").greet("  Ana "));
    }

    @Test
    void refusesABlankName() {
        assertThrows(IllegalArgumentException.class, () -> greeterWith("Hi").greet("  "));
    }
}
