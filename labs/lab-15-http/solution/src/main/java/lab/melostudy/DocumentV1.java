package lab.melostudy;

/** Representation of a document in API version 1. */
public record DocumentV1(Long id, String title, String content, long version) {

    static DocumentV1 of(Document document) {
        return new DocumentV1(document.id, document.title, document.content, document.version);
    }
}
