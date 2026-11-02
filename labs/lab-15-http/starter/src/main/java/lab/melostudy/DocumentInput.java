package lab.melostudy;

/** What a client sends to create or replace a document. */
public record DocumentInput(String title, String content) {
}
