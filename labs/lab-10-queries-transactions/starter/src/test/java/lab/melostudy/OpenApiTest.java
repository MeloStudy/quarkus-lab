package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;

/**
 * Checks the generated OpenAPI document itself: it is the contract other teams read,
 * so it must tell the truth about the API.
 */
@QuarkusTest
class OpenApiTest {

    private static final Set<String> DEFAULT_SUMMARIES = Set.of("List", "Get", "Create", "Update", "Delete");

    private static JsonPath cached;

    /** Fetched lazily: a static @BeforeAll would run before Quarkus has started. */
    private static JsonPath doc() {
        if (cached == null) {
            cached = given().get("/q/openapi?format=json").then().statusCode(200).extract().jsonPath();
        }
        return cached;
    }

    private static Map<String, Object> responses(String path, String method) {
        return doc().getMap("paths.'" + path + "'." + method + ".responses");
    }

    private static String errorSchemaRef(String path, String method, String status) {
        return doc().getString("paths.'" + path + "'." + method + ".responses.'" + status
                + "'.content.'application/json'.schema.'$ref'");
    }

    // ---- Task 1: the document exists

    @Test
    void theDocumentIsServedInYamlAndJson() {
        given().get("/q/openapi").then().statusCode(200).contentType(containsString("yaml"));
        given().get("/q/openapi?format=json").then().statusCode(200).contentType(containsString("json"));
        assertThat(doc().getString("openapi"), not(emptyOrNullString()));
        assertThat(doc().getMap("paths"), hasKey("/products"));
        assertThat(doc().getMap("paths"), hasKey("/products/{id}"));
    }

    @Test
    void swaggerUiIsAvailableDuringDevelopment() {
        given().redirects().follow(true).get("/q/swagger-ui/")
                .then().statusCode(200).body(containsString("swagger"));
    }

    // ---- Task 2: API metadata

    @Test
    void theApiHasATitleVersionAndDescription() {
        assertThat(doc().getString("info.title"), equalTo("Product Catalog API"));
        assertThat(doc().getString("info.version"), equalTo("1.0.0"));
        assertThat(doc().getString("info.description"), not(emptyOrNullString()));
    }

    @Test
    void theResourceIsGroupedUnderADocumentedTag() {
        assertThat(doc().getList("paths.'/products'.get.tags"), hasItem("Products"));
        assertThat(doc().getList("tags.name"), hasItem("Products"));
        assertThat(doc().getString("tags.find { it.name == 'Products' }.description"), not(emptyOrNullString()));
    }

    // ---- Task 3: operations

    @Test
    void everyProductOperationHasAMeaningfulSummary() {
        for (String[] op : new String[][] { { "/products", "get" }, { "/products", "post" },
                { "/products/{id}", "get" }, { "/products/{id}", "put" }, { "/products/{id}", "delete" } }) {
            String summary = doc().getString("paths.'" + op[0] + "'." + op[1] + ".summary");
            assertThat(op[1] + " " + op[0] + " has no summary", summary, notNullValue());
            assertThat(op[1] + " " + op[0] + " still has the generated summary '" + summary + "'",
                    DEFAULT_SUMMARIES.contains(summary), equalTo(false));
        }
    }

    @Test
    void parametersAreDescribed() {
        List<Map<String, Object>> params = doc().getList("paths.'/products'.get.parameters");
        for (Map<String, Object> p : params) {
            assertThat("parameter " + p.get("name") + " has no description", p.get("description"), notNullValue());
        }
    }

    // ---- Task 4: responses tell the truth

    @Test
    void createIsDocumentedAs201NotAs200() {
        assertThat(responses("/products", "post"), hasKey("201"));
        assertThat(responses("/products", "post"), not(hasKey("200")));
        assertThat(doc().getMap("paths.'/products'.post.responses.'201'.headers"), hasKey("Location"));
        assertThat(doc().getString("paths.'/products'.post.responses.'201'.content.'application/json'.schema.'$ref'"),
                equalTo("#/components/schemas/Product"));
    }

    @Test
    void listDocumentsTheTotalCountHeader() {
        assertThat(doc().getMap("paths.'/products'.get.responses.'200'.headers"), hasKey("X-Total-Count"));
    }

    @Test
    void failuresAreDocumentedWithTheErrorSchema() {
        String[][] expected = {
                { "/products", "get", "400" },
                { "/products", "post", "400" }, { "/products", "post", "409" },
                { "/products/{id}", "get", "404" },
                { "/products/{id}", "put", "400" }, { "/products/{id}", "put", "404" }, { "/products/{id}", "put", "409" },
                { "/products/{id}", "delete", "404" } };
        for (String[] e : expected) {
            String ref = errorSchemaRef(e[0], e[1], e[2]);
            // reusable responses may be referenced ($ref to #/components/responses) or inlined
            if (ref == null) {
                String responseRef = doc().getString("paths.'" + e[0] + "'." + e[1] + ".responses.'" + e[2] + "'.'$ref'");
                assertThat(e[1] + " " + e[0] + " " + e[2] + " is not documented", responseRef, notNullValue());
                String name = responseRef.substring(responseRef.lastIndexOf('/') + 1);
                ref = doc().getString("components.responses." + name + ".content.'application/json'.schema.'$ref'");
            }
            assertThat(e[1] + " " + e[0] + " " + e[2], ref, equalTo("#/components/schemas/ApiError"));
        }
    }

    // ---- Task 5: schemas

    @Test
    void errorSchemasArePublished() {
        assertThat(doc().getMap("components.schemas.ApiError.properties").keySet(),
                equalTo(Set.of("code", "message", "errors", "errorId")));
        assertThat(doc().getMap("components.schemas"), hasKey("FieldError"));
    }

    @Test
    void schemaFieldsHaveDescriptionsAndExamples() {
        for (String field : List.of("name", "category", "price")) {
            String base = "components.schemas.ProductInput.properties." + field;
            assertThat(field + " has no description", doc().getString(base + ".description"), not(emptyOrNullString()));
            assertThat(field + " has no example", doc().get(base + ".example") != null || doc().get(base + ".examples") != null,
                    equalTo(true));
        }
        assertThat(doc().getString("components.schemas.Product.description"), not(emptyOrNullString()));
    }

    @Test
    void validationConstraintsShowUpInTheContract() {
        // not annotated for OpenAPI at all: they come from Bean Validation
        assertThat(doc().getInt("components.schemas.ProductInput.properties.name.maxLength"), equalTo(100));
        assertThat(doc().getList("components.schemas.ProductInput.required"), hasItem("price"));
        assertThat(doc().getInt("paths.'/products'.get.parameters.find { it.name == 'size' }.schema.maximum"), equalTo(100));
    }

    // ---- Task 6: a filter for what every operation shares

    @Test
    void everyProductOperationDocumentsTheUnexpectedFailure() {
        for (String[] op : new String[][] { { "/products", "get" }, { "/products", "post" },
                { "/products/{id}", "get" }, { "/products/{id}", "put" }, { "/products/{id}", "delete" } }) {
            assertThat(op[1] + " " + op[0] + " has no 500", errorSchemaRef(op[0], op[1], "500"),
                    equalTo("#/components/schemas/ApiError"));
        }
    }

    @Test
    void theDocumentDoesNotInventAProductsDeleteOnTheCollection() {
        assertThat(doc().getMap("paths.'/products'"), not(hasKey("delete")));
        assertThat(doc().get("paths.'/products'.delete"), nullValue());
    }
}
