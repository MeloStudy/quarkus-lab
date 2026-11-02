package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.ws.rs.Path;

/**
 * Tasks 1 and 2: the persistence model and the API model are separate. Plain reflection, no application
 * needed: (a) entities carry no Jackson annotation, (b) no REST resource method takes or returns an entity.
 */
class EntitiesAreNotTheApiTest {

    private static final List<Class<?>> ENTITIES = List.of(Product.class, Label.class, Review.class,
            CustomerReview.class, CriticReview.class, CustomerOrder.class, PriceChange.class, AuditEntry.class);

    private static final List<Class<?>> RESOURCES = List.of(ProductResource.class, OrderResource.class,
            ReportResource.class);

    @Test
    void theListsAreComplete() {
        // guards the test itself: every @Entity of the application is checked, every resource too
        for (Class<?> entity : ENTITIES) {
            assertTrue(entity.isAnnotationPresent(Entity.class), entity + " is not an entity");
        }
        for (Class<?> resource : RESOURCES) {
            assertTrue(resource.isAnnotationPresent(Path.class), resource + " is not a resource");
        }
    }

    @Test
    void entitiesKnowNothingAboutJson() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> entity : ENTITIES) {
            check(entity, entity, offenders);
            for (Field field : entity.getDeclaredFields()) {
                check(field, entity.getSimpleName() + "." + field.getName(), offenders);
            }
            for (Method method : entity.getDeclaredMethods()) {
                check(method, entity.getSimpleName() + "." + method.getName() + "()", offenders);
            }
        }
        assertTrue(offenders.isEmpty(), "Jackson annotations on entities: " + offenders);
    }

    @Test
    void noResourceMethodExposesAnEntity() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> resource : RESOURCES) {
            for (Method method : resource.getDeclaredMethods()) {
                List<Type> types = new ArrayList<>(List.of(method.getGenericParameterTypes()));
                types.add(method.getGenericReturnType());
                for (Type type : types) {
                    if (mentionsAnEntity(type)) {
                        offenders.add(resource.getSimpleName() + "." + method.getName() + " uses " + type.getTypeName());
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "entities in the API: " + offenders);
    }

    private static void check(AnnotatedElement element, Object where, List<String> offenders) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().getName().startsWith("com.fasterxml.jackson")) {
                offenders.add(where + " has @" + annotation.annotationType().getSimpleName());
            }
        }
    }

    /** True when the type, or any type argument of it (List&lt;Product&gt;, RestResponse&lt;Product&gt;...), is an entity. */
    private static boolean mentionsAnEntity(Type type) {
        if (type instanceof Class<?> c) {
            return c.isAnnotationPresent(Entity.class) || (c.isArray() && mentionsAnEntity(c.getComponentType()));
        }
        if (type instanceof ParameterizedType p) {
            if (mentionsAnEntity(p.getRawType())) {
                return true;
            }
            for (Type argument : p.getActualTypeArguments()) {
                if (mentionsAnEntity(argument)) {
                    return true;
                }
            }
        }
        if (type instanceof GenericArrayType a) {
            return mentionsAnEntity(a.getGenericComponentType());
        }
        return false;
    }
}
