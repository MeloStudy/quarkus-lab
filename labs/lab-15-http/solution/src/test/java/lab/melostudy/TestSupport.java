package lab.melostudy;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Test-only helper: clean state, plus shortcuts to create a document. Raw SQL and raw HTTP on purpose, so the tests
 * do not depend on the code under study. {@code docs.storage.dir} is a test path, never a real one.
 */
@ApplicationScoped
public class TestSupport {

    @ConfigProperty(name = "docs.storage.dir")
    String storageDir;

    @Transactional
    public void reset() {
        Attachment.deleteAll();
        Document.deleteAll();
        Path dir = storage();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(dir)).forEach(p -> p.toFile().delete());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public Path storage() {
        return Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** Creates a document through the API and returns the response (status, ETag header and body included). */
    public static Response createDocument(String title, String content) {
        return given().contentType(ContentType.JSON)
                .body("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}")
                .when().post("/api/v1/documents");
    }

    public static String urlOf(Response created) {
        return "/api/v1/documents/" + created.jsonPath().getLong("id");
    }
}
