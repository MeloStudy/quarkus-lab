package lab.tracker;

import java.time.Instant;

public record CommentResponse(long id, String author, String body, Instant createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.id, c.author, c.body, c.createdAt);
    }
}
