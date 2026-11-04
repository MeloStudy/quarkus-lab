package lab.melostudy;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Greeter {

    @ConfigProperty(name = "greeting.prefix")
    String prefix;

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return prefix + ", " + name.strip() + "!";
    }
}
