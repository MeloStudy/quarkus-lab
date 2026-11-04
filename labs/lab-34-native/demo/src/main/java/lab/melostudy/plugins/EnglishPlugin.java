package lab.melostudy.plugins;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lab.melostudy.Plugin;

@RegisterForReflection
public class EnglishPlugin implements Plugin {

    @Override
    public String greet() {
        return "Hello";
    }
}
