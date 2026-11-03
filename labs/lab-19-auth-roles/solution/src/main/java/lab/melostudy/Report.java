package lab.melostudy;

/** A report written by a user. {@code owner} is the name of the authenticated user who created it. */
public record Report(long id, String owner, String title, String body) {
}
