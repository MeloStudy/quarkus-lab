package lab.melostudy;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** The annotated string must be one of the categories configured in {@link CategoryCatalog}. */
@Documented
@Constraint(validatedBy = AllowedCategoryValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface AllowedCategory {

    String message() default "is not an allowed category";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
