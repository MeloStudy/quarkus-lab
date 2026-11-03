package lab.melostudy;

import java.util.Set;

/** The skus the warehouse can ship. */
public final class Catalog {

    public static final Set<String> SKUS = Set.of("A-100", "B-200", "C-300");

    private Catalog() {
    }
}
