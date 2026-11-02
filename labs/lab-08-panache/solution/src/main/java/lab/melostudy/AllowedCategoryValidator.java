package lab.melostudy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validators are CDI beans in Quarkus, so they can inject other beans.
 * Null and blank values are valid here: {@code @NotBlank} is the one that rejects them,
 * so each problem is reported once.
 */
@ApplicationScoped
public class AllowedCategoryValidator implements ConstraintValidator<AllowedCategory, String> {

    private final CategoryCatalog catalog;

    AllowedCategoryValidator(CategoryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || catalog.isAllowed(value);
    }
}
