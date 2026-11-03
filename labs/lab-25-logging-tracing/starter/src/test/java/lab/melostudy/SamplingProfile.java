package lab.melostudy;

import java.util.HashMap;
import java.util.Map;


/** Keeps none of the traces. The sampler is a build-time setting: this profile makes Quarkus build the application again. */
public class SamplingProfile extends LogProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(super.getConfigOverrides());
        config.put("quarkus.otel.traces.sampler", "traceidratio");
        config.put("quarkus.otel.traces.sampler.arg", "0.0");
        return config;
    }
}
