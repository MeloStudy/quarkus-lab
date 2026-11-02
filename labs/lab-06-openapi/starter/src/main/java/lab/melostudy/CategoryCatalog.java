package lab.melostudy;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/** The categories the business accepts. Configurable: {@code catalog.categories=books,games,music}. */
@ApplicationScoped
public class CategoryCatalog {

    private final Set<String> allowed;

    CategoryCatalog(@ConfigProperty(name = "catalog.categories", defaultValue = "books,games,music") List<String> categories) {
        this.allowed = categories.stream().map(c -> c.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAllowed(String category) {
        return category != null && allowed.contains(category.trim().toLowerCase(Locale.ROOT));
    }
}
