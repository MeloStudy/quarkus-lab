package lab.melostudy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;

/** Task 4: files in and out. Never trust the name, the type or the size that the client announces. */
@QuarkusTest
class UploadTest {

    @Inject
    TestSupport support;

    private String attachments;

    @BeforeEach
    void createADocument() {
        support.reset();
        attachments = TestSupport.urlOf(TestSupport.createDocument("Plan", "v1")) + "/attachments";
    }

    @AfterEach
    void removeWhatATraversalMayHaveLeftBehind() throws IOException {
        Files.deleteIfExists(support.storage().resolve("../evil.txt").normalize());
    }

    private Response upload(String fileName, byte[] bytes, String type) {
        return given().multiPart("file", fileName, bytes, type).post(attachments);
    }

    private long filesInStorage() throws IOException {
        Path dir = support.storage();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        }
    }

    @Test
    void aFileGoesInAndComesOutByteForByte() {
        byte[] bytes = new byte[300 * 1024];
        new Random(42).nextBytes(bytes);

        Response uploaded = upload("report.pdf", bytes, "application/pdf");
        uploaded.then().statusCode(201).body("fileName", equalTo("report.pdf")).body("size", equalTo(bytes.length));

        byte[] downloaded = given().get(uploaded.getHeader("Location")).then().statusCode(200).extract().asByteArray();
        assertArrayEquals(bytes, downloaded);
    }

    @Test
    void theDownloadSaysWhatItIsAndHowToSaveIt() {
        Response uploaded = upload("notes.txt", "hello".getBytes(), "text/plain");

        given().get(uploaded.getHeader("Location")).then().statusCode(200)
                .contentType(containsString("text/plain"))
                .header("Content-Disposition", containsString("attachment"))
                .header("Content-Disposition", containsString("notes.txt"))
                .header("Content-Length", equalTo("5"))
                .body(equalTo("hello"));
    }

    @Test
    void theClientNameNeverDecidesWhereTheFileIsWritten() throws IOException {
        Response uploaded = upload("../evil.txt", "hello".getBytes(), "text/plain");

        uploaded.then().statusCode(201).body("fileName", equalTo("evil.txt"));
        assertFalse(Files.exists(support.storage().resolve("../evil.txt").normalize()), "the file escaped the storage directory");
        try (Stream<Path> files = Files.list(support.storage())) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().equals("evil.txt")),
                    "the file must be stored under a generated name, not the client's");
        }
    }

    @Test
    void twoUploadsWithTheSameNameDoNotOverwriteEachOther() {
        Response first = upload("report.txt", "first".getBytes(), "text/plain");
        Response second = upload("report.txt", "second".getBytes(), "text/plain");

        given().get(first.getHeader("Location")).then().statusCode(200).body(equalTo("first"));
        given().get(second.getHeader("Location")).then().statusCode(200).body(equalTo("second"));
    }

    @Test
    void aHostileFileNameCannotBreakTheDownloadHeader() {
        Response uploaded = upload("a\"; filename=\"b.exe", "x".getBytes(), "text/plain");

        uploaded.then().statusCode(201);
        String disposition = given().get(uploaded.getHeader("Location")).getHeader("Content-Disposition");
        assertEquals(2, disposition.chars().filter(c -> c == '"').count(), "only the two quotes around the name: " + disposition);
    }

    @Test
    void onlyAllowedContentTypesAreAccepted() throws IOException {
        upload("a.txt", "x".getBytes(), "text/plain").then().statusCode(201);
        long before = filesInStorage();

        upload("a.gif", "GIF89a".getBytes(), "image/gif").then().statusCode(415);
        upload("a.html", "<script>".getBytes(), "text/html").then().statusCode(415);

        assertEquals(before, filesInStorage(), "a refused file must not be stored");
    }

    @Test
    void aRequestWithoutTheFilePartIs400() {
        given().multiPart("other", "x").post(attachments).then().statusCode(400);
    }

    @Test
    void aFileOverTheSizeLimitIsNotStored() throws IOException {
        long before = filesInStorage();
        boolean refused;
        try {
            // The limit is quarkus.http.limits.max-body-size. The server may answer 413 or simply drop the connection:
            // both mean "refused"; what must never happen is a 201.
            refused = upload("big.txt", new byte[2 * 1024 * 1024], "text/plain").statusCode() != 201;
        } catch (Exception connectionDropped) { // RestAssured rethrows checked IO errors as they are
            refused = true;
        }
        assertTrue(refused, "a 2 MB body must be refused when the limit is 1 MB");
        assertEquals(before, filesInStorage());
        given().get(attachments).then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    void anAttachmentIsOnlyReachableThroughItsOwnDocument() {
        Response uploaded = upload("a.txt", "x".getBytes(), "text/plain");
        String otherDocument = TestSupport.urlOf(TestSupport.createDocument("Other", "v1"));
        String attachmentId = uploaded.getHeader("Location").substring(uploaded.getHeader("Location").lastIndexOf('/') + 1);

        given().get(otherDocument + "/attachments/" + attachmentId).then().statusCode(404);
    }
}
