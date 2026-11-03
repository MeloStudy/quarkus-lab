package lab.melostudy;

/**
 * What a client may send to create a report. There is no {@code owner}: who writes a report is decided by the
 * authenticated identity, never by the request body.
 */
public record ReportRequest(String title, String body) {
}
