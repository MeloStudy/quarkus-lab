package lab.melostudy;

import io.quarkus.cache.CacheManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Test-only helper: puts upstream and cache in a known state. It talks to the {@link CacheManager} directly
 * instead of calling your {@code forgetAll()}, so a bug in your code cannot hide behind the cleanup.
 */
@ApplicationScoped
public class RateTestSupport {

    @Inject
    RateProvider provider;

    @Inject
    CacheManager caches;

    public void reset() {
        provider.reset();
        // The cache only exists once some method uses it (the starter has none yet)
        caches.getCache("rates").ifPresent(cache -> cache.invalidateAll().await().indefinitely());
    }
}
