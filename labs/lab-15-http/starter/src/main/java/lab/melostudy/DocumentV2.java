package lab.melostudy;

/** Representation of a document in API version 2: {@code title} became {@code name}, and the version moved to the ETag. */
public record DocumentV2(Long id, String name, String content) {

    static DocumentV2 of(Document document) {
        return new DocumentV2(document.id, document.title, document.content);
    }
}
