package lab.melostudy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Documents, API version 1. It works, but it is the naive version of everything this lab is about:
 * TODO task 1: no validators (ETag) on the representations.
 * TODO task 2: a PUT overwrites whatever is there; two people editing lose each other's work.
 * TODO task 4: files are stored under the name the client sends, with whatever type and size.
 */
@jakarta.ws.rs.Path("/api/v1/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    private final FileStorage storage;

    DocumentResource(FileStorage storage) {
        this.storage = storage;
    }

    // ---- documents

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(DocumentInput input) {
        requireValid(input);
        Document document = new Document();
        document.title = input.title();
        document.content = input.content();
        document.persistAndFlush();
        return Response.created(URI.create("/api/v1/documents/" + document.id)).entity(DocumentV1.of(document)).build();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}")
    public DocumentV1 get(@RestPath long id) {
        Document document = Document.findById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        return DocumentV1.of(document);
    }

    @PUT
    @jakarta.ws.rs.Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public DocumentV1 replace(@RestPath long id, DocumentInput input) {
        requireValid(input);
        Document document = Document.findById(id);
        if (document == null) {
            throw new NotFoundException();
        }
        document.title = input.title();
        document.content = input.content();
        document.flush();
        return DocumentV1.of(document);
    }

    private static void requireValid(DocumentInput input) {
        if (input == null || input.title() == null || input.title().isBlank() || input.content() == null) {
            throw new BadRequestException("title and content are required");
        }
    }

    // ---- attachments

    @POST
    @jakarta.ws.rs.Path("/{id}/attachments")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public RestResponse<AttachmentInfo> upload(@RestPath long id, @RestForm("file") FileUpload file) throws IOException {
        if (Document.findById(id) == null) {
            throw new NotFoundException();
        }
        Attachment attachment = new Attachment();
        attachment.documentId = id;
        attachment.fileName = file.fileName();
        attachment.contentType = file.contentType();
        attachment.size = file.size();
        attachment.storedName = file.fileName(); // TODO task 4: the client's name decides where the file is written
        Files.createDirectories(storage.directory());
        Files.move(file.uploadedFile(), storage.directory().resolve(attachment.storedName),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        attachment.persist();
        return RestResponse.ResponseBuilder
                .<AttachmentInfo>created(URI.create("/api/v1/documents/" + id + "/attachments/" + attachment.id))
                .entity(AttachmentInfo.of(attachment)).build();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}/attachments")
    public List<AttachmentInfo> attachments(@RestPath long id) {
        if (Document.findById(id) == null) {
            throw new NotFoundException();
        }
        return Attachment.of(id).stream().map(AttachmentInfo::of).toList();
    }

    @GET
    @jakarta.ws.rs.Path("/{id}/attachments/{attachmentId}")
    @Produces(MediaType.WILDCARD)
    public RestResponse<File> download(@RestPath long id, @RestPath long attachmentId) {
        Attachment attachment = Attachment.findById(attachmentId);
        if (attachment == null) {
            throw new NotFoundException();
        }
        return RestResponse.ResponseBuilder.ok(storage.locate(attachment.storedName).toFile(), attachment.contentType).build();
    }

    public record AttachmentInfo(Long id, String fileName, String contentType, long size) {
        static AttachmentInfo of(Attachment attachment) {
            return new AttachmentInfo(attachment.id, attachment.fileName, attachment.contentType, attachment.size);
        }
    }
}
